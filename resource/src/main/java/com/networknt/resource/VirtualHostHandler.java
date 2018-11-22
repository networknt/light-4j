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
