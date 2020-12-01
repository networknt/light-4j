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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.body.BodyHandler;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.handler.LightHttpHandler;
import com.networknt.httpstring.ContentType;
import com.networknt.logging.model.LoggerConfig;
import com.networknt.logging.model.LoggerInfo;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * This handler will change the logging level for the given Logger. Ex. ERROR to DEBUG
 *
 */
public class LoggerPostHandler implements LightHttpHandler {

    public static final String CONFIG_NAME = "logging";
    static final String STATUS_LOGGER_INFO_DISABLED = "ERR12108";

    public LoggerPostHandler() {
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        List loggers = (List) exchange.getAttachment(BodyHandler.REQUEST_BODY);
        LoggerConfig config = (LoggerConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, LoggerConfig.class);

        if (config.isEnabled()) {
            for (Object object : loggers) {
                Map<String, String> map = (Map<String, String>) object;
                String name = map.get("name");
                Level level = Level.valueOf(map.get("level"));
                ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(name);
                if(level != logger.getLevel()) logger.setLevel(level);
            }

            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, ContentType.APPLICATION_JSON.value());
            exchange.getResponseSender().send(JsonMapper.toJson(loggers));
        } else {
            logger.error("Logging is disabled in logging.yml");
            setExchangeStatus(exchange, STATUS_LOGGER_INFO_DISABLED);
        }
    }
}
