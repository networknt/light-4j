package com.networknt.handler.config;

import java.util.List;

public class NamedRequestChain {
    private String name;
    private List<Object> middleware;
    private String endPoint;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    public List<Object> getMiddleware() {
        return middleware;
    }

    public void setMiddleware(List<Object> middleware) {
        this.middleware = middleware;
    }
}
