package com.networknt.handler.config;

import java.util.List;

/**
 * @author Nicholas Azar
 */
public class HandlerPath {
    private String path;
    private String httpVerb;
    private List<Object> middleware;
    private Object endPoint;
    private String namedMiddlewareChain;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getHttpVerb() {
        return httpVerb;
    }

    public void setHttpVerb(String httpVerb) {
        this.httpVerb = httpVerb;
    }

    public List<Object> getMiddleware() {
        return middleware;
    }

    public void setMiddleware(List<Object> middleware) {
        this.middleware = middleware;
    }

    public Object getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(Object endPoint) {
        this.endPoint = endPoint;
    }

    public String getNamedMiddlewareChain() {
        return namedMiddlewareChain;
    }

    public void setNamedMiddlewareChain(String namedMiddlewareChain) {
        this.namedMiddlewareChain = namedMiddlewareChain;
    }
}
