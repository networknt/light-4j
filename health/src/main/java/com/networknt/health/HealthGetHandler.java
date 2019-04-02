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

package com.networknt.health;

import com.networknt.handler.LightHttpHandler;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a server health handler that output OK to indicate the server is alive. Normally,
 * it will be use by F5 to check if the server is health before route request to it. Another
 * way to check server health is to ping the ip and port and it is the standard way to check
 * server status for F5. However, the service instance is up and running doesn't mean it is
 * functioning. This is the reason to provide a this handler to output more information about
 * the server for F5 or maybe in the future for the API marketplace.
 *
 * Note that we only recommend to use F5 as reverse proxy for services with static IP addresses
 * that act like traditional web server. These services will be sitting in DMZ to serve mobile
 * native and browser SPA and aggregate other services in the backend. For services deployed
 * in the cloud dynamically, there is no reverse proxy but using client side service discovery.
 *
 * TODO support cascade health check with configuration, for example, database etc.
 *
 * @author Steve Hu
 */
public class HealthGetHandler implements LightHttpHandler {
    public static final String CONFIG_NAME = "health";

    static final Logger logger = LoggerFactory.getLogger(HealthGetHandler.class);

    public HealthGetHandler(){}

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        exchange.getResponseSender().send("OK");
    }
}
