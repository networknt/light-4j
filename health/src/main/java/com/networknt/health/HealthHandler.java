/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
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

import com.networknt.config.Config;
import com.networknt.status.Status;
import com.networknt.utility.ModuleRegistry;
import com.networknt.utility.Util;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * This is a server health handler that output OK to indicate the server is alive.
 * TODO support cascade health check
 *
 * Created by steve on 17/09/16.
 */
public class HealthHandler implements HttpHandler {
    public static final String CONFIG_NAME = "health";

    static final Logger logger = LoggerFactory.getLogger(HealthHandler.class);

    public HealthHandler(){}

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        exchange.getResponseSender().send("OK");
    }
}
