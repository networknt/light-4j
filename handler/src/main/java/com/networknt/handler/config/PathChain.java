package com.networknt.handler.config;

import com.networknt.utility.NetUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Nicholas Azar
 */
public class PathChain {
    private String path;
    private String method;
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

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) throws Exception {
        if (method.contains(",")) {
            List<String> upperMethods = Arrays.stream(method.split(",")).map(String::toUpperCase).collect(Collectors.toList());
            if (!NetUtils.METHODS.containsAll(upperMethods)) {
                throw new Exception("Invalid HTTP methods provided: " + method);
            } else{
                this.method = method;
            }
        } else if (NetUtils.METHODS.contains(method.toUpperCase())) {
            this.method = method;
        } else {
            throw new Exception("Invalid HTTP method provided: " + method);
        }
    }
}
