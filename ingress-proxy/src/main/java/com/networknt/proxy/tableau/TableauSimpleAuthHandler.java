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

package com.networknt.proxy.tableau;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.client.Http2Client;
import com.networknt.common.DecryptUtil;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This is a very simple Tableau authentication handler that doesn't cache any token, a request
 * comes in, this handler will get the contentUrl from the header and login to Tableau with the
 * username and password from config file. Once the token is retrieved, it will be saved into the
 * exchange attachment for the subsequent request to access Tableau server.
 *
 * This is the simplest implementation but not perform well if you have too many request. It should
 * only be used with batch client or very light load.
 *
 * @author Steve Hu
 *
 */
public class TableauSimpleAuthHandler implements MiddlewareHandler {
    private static final Logger logger = LoggerFactory.getLogger(TableauSimpleAuthHandler.class);
    private static final String TABLEAU_CONFIG_NAME = "tableau";
    private static final String SECRET_CONFIG_NAME = "secret";
    private static final String MISSING_TABLEAU_CONTENT_URL = "ERR11301";
    private static final String FAIL_TO_GET_TABLEAU_TOKEN = "ERR11300";

    private static final HttpString TABLEAU_TOKEN = new HttpString("X-Tableau-Auth");
    private static final HttpString TABLEAU_CONTENT_URL = new HttpString("tableauContentUrl");

    private static final TableauConfig config =
            (TableauConfig) Config.getInstance().getJsonObjectConfig(TABLEAU_CONFIG_NAME, TableauConfig.class);
    private static final Map<String, Object> secretConfig;

    private volatile HttpHandler next;

    static {
        Map<String, Object> secretMap = Config.getInstance().getJsonMapConfig(SECRET_CONFIG_NAME);
        if(secretMap != null) {
            secretConfig = DecryptUtil.decryptMap(secretMap);
        } else {
            throw new ExceptionInInitializerError("Could not locate secret.yml");
        }
    }

    public TableauSimpleAuthHandler() {

    }

    /**
     * Get the credentials from tableau config and send a request to Tableau server to get the token.
     * The token will be saved into exchange attachment so that the next handler in the chain can use
     * it to access to the target server.
     *
     * @param exchange http exchange
     * @throws Exception exception
     */
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String contentUrl = exchange.getRequestHeaders().getFirst(TABLEAU_CONTENT_URL);
        if(contentUrl == null || contentUrl.length() == 0) {
            setExchangeStatus(exchange, MISSING_TABLEAU_CONTENT_URL);
            return;
        }
        String token = getToken(contentUrl);
        if(token == null) {
            setExchangeStatus(exchange, FAIL_TO_GET_TABLEAU_TOKEN);
            return;
        }
        exchange.getRequestHeaders().put(TABLEAU_TOKEN, token);
        exchange.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
        Handler.next(exchange, next);
    }

    private String getToken(String contentUrl) throws ClientException {
        String token = null;
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            // use HTTP 1.1 connection as I don't think Tableau supports HTTP 2.0
            connection = client.connect(new URI(config.getServerUrl()), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.EMPTY).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            final String requestBody = getRequestBody(contentUrl);
            ClientRequest request = new ClientRequest().setPath(config.getServerPath()).setMethod(Methods.POST);
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            request.getRequestHeaders().put(Headers.HOST, "localhost");
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
            connection.sendRequest(request, client.createClientCallback(reference, latch, requestBody));
            latch.await();
            int statusCode = reference.get().getResponseCode();
            if(logger.isDebugEnabled()) logger.debug("statusCode = " + statusCode);
            if(statusCode == StatusCodes.OK) {
                String responseBody = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
                if(logger.isDebugEnabled()) logger.debug("responseBody = " + responseBody);
                Map<String, Object> responseMap = Config.getInstance().getMapper().readValue(responseBody, new TypeReference<Map<String, Object>>() {});
                Map<String, Object> credentials = (Map<String, Object>)responseMap.get("credentials");
                token = (String)credentials.get("token");
            }
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        return token;
    }

    private String getRequestBody(String contentUrl) throws IOException {
        Map<String, Object> site = new HashMap<>();
        site.put("contentUrl", contentUrl);
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("name", config.getTableauUsername());
        credentials.put("password", secretConfig.get("tableauPassword"));
        credentials.put("site", site);
        Map<String, Object> request = new HashMap<>();
        request.put("credentials", credentials);
        return Config.getInstance().getMapper().writeValueAsString(request);
    }

    @Override
    public HttpHandler getNext() {
        return next;
    }

    @Override
    public MiddlewareHandler setNext(HttpHandler next) {
        Handlers.handlerNotNull(next);
        this.next = next;
        return this;
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(TableauSimpleAuthHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(TABLEAU_CONFIG_NAME), null);
    }

}
