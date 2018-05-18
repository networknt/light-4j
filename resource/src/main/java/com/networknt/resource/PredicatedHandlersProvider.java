package com.networknt.resources;

import io.undertow.server.handlers.builder.PredicatedHandler;

import java.util.List;

/**
 * @author Nicholas Azar
 * Created on April 21, 2018
 */
public interface PredicatedHandlersProvider {
    List<PredicatedHandler> getPredicatedHandlers();
}
