package com.networknt.handler.config;

import java.util.List;

/**
 * @author Nicholas Azar
 */
public class PathChain {
    private String path;
    private String requestType;
    private List<String> exec;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getExec() {
        return exec;
    }

    public void setExec(List<String> exec) {
        this.exec = exec;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }
}
