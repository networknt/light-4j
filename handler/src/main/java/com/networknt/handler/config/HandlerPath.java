package com.networknt.handler.config;

import java.util.List;

public class HandlerPath {
    private String path;
    private String httpVerb;
    private List<String> middleware;
    private String endPoint;
    private String namedRequestChain;

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

    public List<String> getMiddleware() {
        return middleware;
    }

    public void setMiddleware(List<String> middleware) {
        this.middleware = middleware;
    }

    public String getNamedRequestChain() {
        return namedRequestChain;
    }

    public void setNamedRequestChain(String namedRequestChain) {
        this.namedRequestChain = namedRequestChain;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }
}
