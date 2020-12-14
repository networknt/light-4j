package com.networknt.status;

import io.undertow.server.HttpServerExchange;

/**
 * Interface to allow custom Status and then inject the customized status into framework through SingletonFactory
 * <p>
 * Implement the interface and configure in service.yml
 * - com.networknt.status.StatusWrapper
 *  - custom implementation
 *
 * @author Jiachen Sun
 */

public interface StatusWrapper {
    /**
     * Encapsulate the default status of the framework as a custom status
     *
     * @param status   The status to be wrapped
     * @param exchange The source of custom info used to wrap status
     * @return the custom status
     */
    Status wrap(Status status, HttpServerExchange exchange);
}
