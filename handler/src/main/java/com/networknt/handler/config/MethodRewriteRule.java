package com.networknt.handler.config;

public class MethodRewriteRule {
    String requestPath;
    String sourceMethod;
    String targetMethod;

    public MethodRewriteRule(String requestPath, String sourceMethod, String targetMethod) {
        this.requestPath = requestPath;
        this.sourceMethod = sourceMethod;
        this.targetMethod = targetMethod;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public String getSourceMethod() {
        return sourceMethod;
    }

    public void setSourceMethod(String sourceMethod) {
        this.sourceMethod = sourceMethod;
    }

    public String getTargetMethod() {
        return targetMethod;
    }

    public void setTargetMethod(String targetMethod) {
        this.targetMethod = targetMethod;
    }
}
