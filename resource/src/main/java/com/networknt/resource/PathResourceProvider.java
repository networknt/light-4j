package com.networknt.resource;

import io.undertow.server.handlers.resource.ResourceManager;

/**
 * @author Nicholas Azar
 * Created on April 21, 2018
 */
public interface PathResourceProvider {
    String getPath();
    Boolean isPrefixPath();
    ResourceManager getResourceManager();
}
