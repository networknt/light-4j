package com.networknt.status;

/**
 * Interface to allow custom Status and then inject the customized status into framework through SingletonFactory
 * <p>
 * Implement the interface and configure in service.yml
 * - com.networknt.status.StatusWrapper
 *   - custom implementation class package
 *
 * @author Jiachen Sun
 */

public interface StatusWrapper {
    /**
     * Encapsulate the default status of the framework as a custom status. If you want to add information from
     * the HttpServerExchange, you can use the exchange parameter. However, it is passed as an Object, so you
     * need to cast it to the actual type in your implementation class.
     *
     * @param status   The status to be wrapped
     * @param exchange The source of custom info used to wrap status
     * @return the custom status
     */
    Status wrap(Status status, Object exchange);
}
