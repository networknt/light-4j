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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.config.Config;
import com.networknt.handler.LightHttpHandler;
import com.networknt.httpstring.ContentType;
import com.networknt.logging.model.LoggerConfig;
import com.networknt.logging.model.LoggerInfo;
import io.undertow.server.HttpServerExchange;

import java.util.Deque;
import java.util.Map;

import io.undertow.util.Headers;
import org.slf4j.LoggerFactory;

/**
 *
 * This handler will provide the logging level for the given Logger.
 */
public class LoggerGetHandler implements LightHttpHandler {

    public static final String CONFIG_NAME = "logging";
    private static final String LOGGER_NAME = "loggerName";
    static final String STATUS_LOGGER_INFO_DISABLED = "ERR12108";
    private static final ObjectMapper mapper = Config.getInstance().getMapper();

    public LoggerGetHandler() {
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {

        Map<String, Deque<String>> parameters = exchange.getQueryParameters();
        String loggerName = parameters.get(LOGGER_NAME).getFirst();
        LoggerConfig config = (LoggerConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, LoggerConfig.class);

        if (config.isEnabled()) {
            ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(loggerName);
            LoggerInfo loggerInfo = new LoggerInfo();
            loggerInfo.setName(logger.getName());
            loggerInfo.setLevel(logger.getLevel().toString());

            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, ContentType.APPLICATION_JSON.value());
            exchange.getResponseSender().send(mapper.writeValueAsString(loggerInfo));
        } else {
            logger.error("Logging is disabled in logging.yml");
            setExchangeStatus(exchange, STATUS_LOGGER_INFO_DISABLED);
        }
    }
}
