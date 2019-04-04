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

import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.config.Config;
import com.networknt.handler.LightHttpHandler;
import com.networknt.httpstring.ContentType;
import com.networknt.logging.model.LoggerConfig;
import com.networknt.logging.model.LoggerInfo;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This handler will provide all the available logging levels for the all loggers.
 */
public class LoggersGetHandler implements LightHttpHandler {

    public static final String CONFIG_NAME = "logging";
    static final String STATUS_LOGGER_INFO_DISABLED = "ERR12108";
    private static final ObjectMapper mapper = Config.getInstance().getMapper();

    public LoggersGetHandler() {
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        List<LoggerInfo> loggersList = new ArrayList<LoggerInfo>();
        LoggerConfig config = (LoggerConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, LoggerConfig.class);

        if (config.isEnabled()) {
            LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
            for (ch.qos.logback.classic.Logger log : lc.getLoggerList()) {
                if (log.getLevel() != null) {
                    LoggerInfo loggerInfo = new LoggerInfo();
                    loggerInfo.setName(log.getName());
                    loggerInfo.setLevel(log.getLevel().toString());
                    loggersList.add(loggerInfo);
                }
            }
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, ContentType.APPLICATION_JSON.value());
            exchange.getResponseSender().send(mapper.writeValueAsString(loggersList));
        } else {
            logger.error("Logging is disabled in logging.yml");
            setExchangeStatus(exchange, STATUS_LOGGER_INFO_DISABLED);
        }
    }
}
