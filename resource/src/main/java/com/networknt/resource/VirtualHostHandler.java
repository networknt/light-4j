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

package com.networknt.resource;

import com.networknt.config.Config;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.NameVirtualHostHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;

import java.nio.file.Paths;

/**
 * This is a wrapper for the NameVirtualHostHandler with configuration support.
 *
 * @author Steve Hu
 */
public class VirtualHostHandler implements HttpHandler {

    NameVirtualHostHandler virtualHostHandler;

    public VirtualHostHandler() {
        VirtualHostConfig config = (VirtualHostConfig)Config.getInstance().getJsonObjectConfig(VirtualHostConfig.CONFIG_NAME, VirtualHostConfig.class);
        virtualHostHandler = new NameVirtualHostHandler();
        for(VirtualHost host: config.hosts) {
            virtualHostHandler.addHost(host.domain, new PathHandler().addPrefixPath(host.getPath(), new ResourceHandler((new PathResourceManager(Paths.get(host.getBase()), host.getTransferMinSize()))).setDirectoryListingEnabled(host.isDirectoryListingEnabled())));
        }
    }

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
        virtualHostHandler.handleRequest(httpServerExchange);
    }
}
