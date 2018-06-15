package com.networknt.handler.config;

import java.util.List;

public class NamedRequestChain {
    private String name;
    private List<String> middleware;
    private String endPoint;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getMiddleware() {
        return middleware;
    }

    public void setMiddleware(List<String> middleware) {
        this.middleware = middleware;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }
}
