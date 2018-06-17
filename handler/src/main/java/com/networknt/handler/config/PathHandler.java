package com.networknt.handler.config;

import java.util.List;

/**
 * @author Nicholas Azar
 */
public class PathHandler {
    private String handlerName;
    private List<HandlerPath> paths;


    public List<HandlerPath> getPaths() {
        return paths;
    }

    public void setPaths(List<HandlerPath> paths) {
        this.paths = paths;
    }

    public String getHandlerName() {
        return handlerName;
    }

    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }
}
