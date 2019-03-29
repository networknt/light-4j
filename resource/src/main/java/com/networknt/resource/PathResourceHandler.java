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
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;

import java.nio.file.Paths;

/**
 * This is a wrapper of PathHandler of Undertow with external configuration. The config
 * name for this handler is path-resource.yml and the base should be an absolute path
 * in your docker volume mapping.
 *
 * @author Steve Hu
 */
public class PathResourceHandler implements HttpHandler {

    PathHandler pathHandler;

    public PathResourceHandler() {
        PathResourceConfig config = (PathResourceConfig)Config.getInstance().getJsonObjectConfig(PathResourceConfig.CONFIG_NAME, PathResourceConfig.class);
        if(config.isPrefix()) {
            pathHandler = new PathHandler()
                    .addPrefixPath(config.getPath(), new ResourceHandler(new PathResourceManager(Paths.get(config.getBase()), config.getTransferMinSize()))
                            .setDirectoryListingEnabled(config.isDirectoryListingEnabled()));
        } else {
            pathHandler = new PathHandler()
                    .addExactPath(config.getPath(), new ResourceHandler(new PathResourceManager(Paths.get(config.getBase()), config.getTransferMinSize()))
                            .setDirectoryListingEnabled(config.isDirectoryListingEnabled()));
        }
    }

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
        pathHandler.handleRequest(httpServerExchange);
    }
}
