package com.networknt.handler.config;

import java.util.List;

/**
 * @author Nicholas Azar
 */
public class NamedMiddlewareChain {
    private String name;
    private List<Object> middleware;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Object> getMiddleware() {
        return middleware;
    }

    public void setMiddleware(List<Object> middleware) {
        this.middleware = middleware;
    }
}
