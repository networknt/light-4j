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

package com.networknt.info;

import com.networknt.config.Config;
import com.networknt.handler.LightHttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * This is a server info handler that output the runtime info about the server. For example, how many
 * components are installed and what is the configuration of each component. For handlers, it is registered
 * when injecting into the handler chain during server startup. For other utilities, it should have a
 * static block to register itself during server startup. Additional info is gathered from environment
 * variable and JVM.
 *
 * @author Steve Hu
 */
public class ServerInfoGetHandler implements LightHttpHandler {
    static final String STATUS_SERVER_INFO_DISABLED = "ERR10013";

    static final Logger logger = LoggerFactory.getLogger(ServerInfoGetHandler.class);
    private String configName = ServerInfoConfig.CONFIG_NAME;

    public ServerInfoGetHandler() {
        if(logger.isDebugEnabled()) logger.debug("ServerInfoGetHandler is constructed");
    }

    public ServerInfoGetHandler(String configName) {
        this.configName = configName;
        if(logger.isDebugEnabled()) logger.debug("ServerInfoGetHandler is constructed with {}", configName);
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        ServerInfoConfig config = ServerInfoConfig.load(configName);
        if(config.isEnableServerInfo()) {
            Map<String,Object> infoMap = ServerInfoUtil.getServerInfo(config);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(infoMap));
        } else {
            setExchangeStatus(exchange, STATUS_SERVER_INFO_DISABLED);
        }
    }


}
