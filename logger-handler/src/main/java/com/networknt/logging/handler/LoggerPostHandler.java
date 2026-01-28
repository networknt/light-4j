/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.logging.handler;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.client.Http2Client;
import com.networknt.client.simplepool.SimpleConnectionHolder;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.handler.LightHttpHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.common.ContentType;
import com.networknt.logging.model.LoggerConfig;
import com.networknt.logging.model.LoggerInfo;
import com.networknt.monad.Failure;
import com.networknt.monad.Result;
import com.networknt.monad.Success;
import com.networknt.status.Status;
import com.networknt.utility.Constants;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This handler will change the logging level for the given Logger. Ex. ERROR to DEBUG
 *
 */
public class LoggerPostHandler implements LightHttpHandler {
    public static final String CONFIG_NAME = "logging";
    static final String LOGGER_INFO_DISABLED = "ERR12108";
    static final String REQUEST_BODY_MISSING = "ERR10059";
    static final String GENERIC_EXCEPTION = "ERR10014";
    static final String API_ERROR_RESPONSE = "ERR10083";
    static final String DOWNSTREAM_ADMIN_DISABLED = "ERR10084";
    public LoggerPostHandler() {
        if(logger.isInfoEnabled()) logger.info("LoggerPostHandler is constructed.");
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        LoggerConfig config = LoggerConfig.load();
        if(!config.isEnabled()) {
            if(logger.isDebugEnabled()) logger.debug("logging is disabled");
            setExchangeStatus(exchange, LOGGER_INFO_DISABLED);
            return;
        }

        // first, we need to check if the X-Adm-PassThrough header exists and if it is true.
        // If it is true, we will just pass the request to the downstream API.
        HeaderValues passThroughObject = exchange.getRequestHeaders().get(Constants.ADM_PASSTHROUGH_STRING);
        List loggers = (List)exchange.getAttachment(AttachmentConstants.REQUEST_BODY);
        if(passThroughObject != null && passThroughObject.getFirst().equals("true")) {
            if(logger.isDebugEnabled()) logger.debug("pass through is true");
            if(config.isDownstreamEnabled()) {
                Result<List<LoggerInfo>> loggersResult = postBackendLogger(loggers, config);
                if(loggersResult.isSuccess()) {
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, ContentType.APPLICATION_JSON.value());
                    exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(loggersResult.getResult()));
                } else {
                    setExchangeStatus(exchange, API_ERROR_RESPONSE, loggersResult.getError());
                }
            } else {
                // need to return an error status to indicate that downstream API access is disabled.
                setExchangeStatus(exchange, DOWNSTREAM_ADMIN_DISABLED);
            }
        } else {
            if(logger.isDebugEnabled()) logger.debug("pass through is false");
            if(loggers == null) {
                logger.error("loggers is null from the attachment. Most likely, the body handler is not in the handler chain.");
                setExchangeStatus(exchange, REQUEST_BODY_MISSING);
            } else {
                for (Object object : loggers) {
                    Map<String, String> map = (Map<String, String>) object;
                    String name = map.get("name");
                    Level level = Level.valueOf(map.get("level"));
                    ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(name);
                    if(level != logger.getLevel()) logger.setLevel(level);
                }
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, ContentType.APPLICATION_JSON.value());
                // send the same loggers back to the client.
                exchange.getResponseSender().send(JsonMapper.toJson(loggers));
            }
        }
    }

    /**
     * Try to access the downstream API logger post endpoint to update the current logging level for loggers. The passed in
     * loggers are the ones that need to be updated, and it is a list. For some backend framework, we need to access multiple
     * endpoints to get all the loggers updated.
     *
     * @return result String of loggers or status error.
     */
    private Result<List<LoggerInfo>> postBackendLogger(List loggers, LoggerConfig config) {
        long start = System.currentTimeMillis();
        Result<List<LoggerInfo>> result = null;
        String framework = config.getDownstreamFramework();
        switch (framework) {
            case Constants.SPRING_BOOT:
                // keep a copy of loggers as the result. This list contains only the updated logger.
                List<LoggerInfo> resultLoggers = new ArrayList<>();
                for(Object object: loggers) {
                    Map<String, String> map = (Map<String, String>) object;
                    String name = map.get("name");
                    String level = map.get("level");
                    boolean r = postSpringBootLogger(name, level, config);
                    if(r) {
                        LoggerInfo loggerInfo = new LoggerInfo();
                        loggerInfo.setName(name);
                        loggerInfo.setLevel(level);
                        resultLoggers.add(loggerInfo);
                    }
                }
                result = Success.of(resultLoggers);
                break;
            default:
                // light-4j response will be used directly.
                result = postLight4jLogger(loggers, config);
        }
        long responseTime = System.currentTimeMillis() - start;
        if(logger.isDebugEnabled()) logger.debug("Downstream health check response time = " + responseTime);
        return result;
    }

    private Result<List<LoggerInfo>> postLight4jLogger(List<LoggerInfo> loggers, LoggerConfig config) {
        String requestPath = "/adm/logger";
        long start = System.currentTimeMillis();
        SimpleConnectionHolder.ConnectionToken connectionToken = null;
        try {
            if(config.getDownstreamHost().startsWith(Constants.HTTPS)) {
                connectionToken = Http2Client.getInstance().borrow(new URI(config.getDownstreamHost()), Http2Client.WORKER, Http2Client.getInstance().getDefaultXnioSsl(), Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true));
            } else {
                connectionToken = Http2Client.getInstance().borrow(new URI(config.getDownstreamHost()), Http2Client.WORKER, Http2Client.BUFFER_POOL, OptionMap.EMPTY);
            }
            ClientConnection connection = (ClientConnection) connectionToken.getRawConnection();
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<ClientResponse> reference = new AtomicReference<>();

            ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(requestPath);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            connection.sendRequest(request, Http2Client.getInstance().createClientCallback(reference, latch));
            latch.await();
            int statusCode = reference.get().getResponseCode();
            String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
            if(logger.isDebugEnabled()) logger.debug("statusCode = " + statusCode + " body  = " + body);
            if(statusCode >= 400) {
                // error response from the backend API.
                return Failure.of(new Status(API_ERROR_RESPONSE, statusCode));
            }
            long responseTime = System.currentTimeMillis() - start;
            if(logger.isDebugEnabled()) logger.debug("Downstream health check response time = " + responseTime);
            if(logger.isDebugEnabled()) logger.debug("Downstream health check response time = " + responseTime);
            return Success.of(getFrameworkLoggers(body, config));
        } catch (Exception ex) {
            logger.error("Could not create connection to the backend: " + config.getDownstreamHost() + ":", ex);
            Status status = new Status(GENERIC_EXCEPTION, ex.getMessage());
            return Failure.of(status);
        } finally {
            Http2Client.getInstance().restore(connectionToken);
        }
    }

    private Boolean postSpringBootLogger(String loggerName, String level, LoggerConfig config) {
        long start = System.currentTimeMillis();
        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("configuredLevel", level);
        SimpleConnectionHolder.ConnectionToken connectionToken = null;
        try {
            if(config.getDownstreamHost().startsWith(Constants.HTTPS)) {
                connectionToken = Http2Client.getInstance().borrow(new URI(config.getDownstreamHost()), Http2Client.WORKER, Http2Client.getInstance().getDefaultXnioSsl(), Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true));
            } else {
                connectionToken = Http2Client.getInstance().borrow(new URI(config.getDownstreamHost()), Http2Client.WORKER, Http2Client.BUFFER_POOL, OptionMap.EMPTY);
            }
            ClientConnection connection = (ClientConnection) connectionToken.getRawConnection();
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<ClientResponse> reference = new AtomicReference<>();

            ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath("/actuator/loggers/" + loggerName);
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            connection.sendRequest(request, Http2Client.getInstance().createClientCallback(reference, latch, Config.getInstance().getMapper().writeValueAsString(requestBodyMap)));
            latch.await();
            int statusCode = reference.get().getResponseCode();
            String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
            if(logger.isDebugEnabled()) logger.debug("statusCode = " + statusCode + " body  = " + body);
            long responseTime = System.currentTimeMillis() - start;
            if(logger.isDebugEnabled()) logger.debug("Downstream health check response time = " + responseTime);
            // error response from the backend API based on the status code.
            return statusCode < 400;
        } catch (Exception ex) {
            logger.error("Could not create connection to the backend: " + config.getDownstreamHost() + ":", ex);
            return false;
        } finally {
            Http2Client.getInstance().restore(connectionToken);
        }
    }

    private static List<LoggerInfo> getFrameworkLoggers(String responseBody, LoggerConfig config) throws Exception {
        String framework = config.getDownstreamFramework();
        switch (framework) {
            case Constants.SPRING_BOOT:
                // transform the response from the spring boot actuator to the LoggerInfo object.
                List<LoggerInfo> loggersList = new ArrayList<LoggerInfo>();
                Map<String, Object> map = Config.getInstance().getMapper().readValue(responseBody, new TypeReference<Map<String, Object>>() {});
                Map<String, Object> loggers = (Map<String, Object>) map.get("loggers");
                for(Map.Entry<String, Object> entry: loggers.entrySet()) {
                    LoggerInfo loggerInfo = new LoggerInfo();
                    loggerInfo.setName(entry.getKey());
                    Map<String, Object> logger = (Map<String, Object>) entry.getValue();
                    loggerInfo.setLevel((String) logger.get("effectiveLevel"));
                    loggersList.add(loggerInfo);
                }
                return loggersList;
            default:
                // light-4j response will be used directly.
                return Config.getInstance().getMapper().readValue(responseBody, new TypeReference<List<LoggerInfo>>() {});
        }
    }

}
