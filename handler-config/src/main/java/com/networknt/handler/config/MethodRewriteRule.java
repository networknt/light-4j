package com.networknt.handler.config;

/**
 * Method rewrite rule
 */
public class MethodRewriteRule {
    String requestPath;
    String sourceMethod;
    String targetMethod;

    /**
     * Constructor
     * @param requestPath request path
     * @param sourceMethod source method
     * @param targetMethod target method
     */
    public MethodRewriteRule(String requestPath, String sourceMethod, String targetMethod) {
        this.requestPath = requestPath;
        this.sourceMethod = sourceMethod;
        this.targetMethod = targetMethod;
    }

    /**
     * Get the request path
     * @return request path
     */
    public String getRequestPath() {
        return requestPath;
    }

    /**
     * Set the request path
     * @param requestPath request path
     */
    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    /**
     * Get the source method
     * @return source method
     */
    public String getSourceMethod() {
        return sourceMethod;
    }

    /**
     * Set the source method
     * @param sourceMethod source method
     */
    public void setSourceMethod(String sourceMethod) {
        this.sourceMethod = sourceMethod;
    }

    /**
     * Get the target method
     * @return target method
     */
    public String getTargetMethod() {
        return targetMethod;
    }

    /**
     * Set the target method
     * @param targetMethod target method
     */
    public void setTargetMethod(String targetMethod) {
        this.targetMethod = targetMethod;
    }
}
