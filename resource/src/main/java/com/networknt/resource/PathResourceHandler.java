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
