package com.networknt.proxy.salesforce;

/**
 * This is an object that contains all the authentication info for each path prefix in the pathPrefixAuth config
 * section. By making it a list of objects, we can support unlimited number of APIs with different authentication
 * parameters.
 */
public class PathPrefixAuth {
    String pathPrefix;
    String authIssuer;
    String authSubject;
    String authAudience;
    String iv;
    int tokenTtl;
    int waitLength;
    String tokenUrl;
    String serviceHost;

    long expiration;
    String accessToken;

    public PathPrefixAuth() {
    }

    public String getPathPrefix() {
        return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    public String getAuthIssuer() {
        return authIssuer;
    }

    public void setAuthIssuer(String authIssuer) {
        this.authIssuer = authIssuer;
    }

    public String getAuthSubject() {
        return authSubject;
    }

    public void setAuthSubject(String authSubject) {
        this.authSubject = authSubject;
    }

    public String getAuthAudience() {
        return authAudience;
    }

    public void setAuthAudience(String authAudience) {
        this.authAudience = authAudience;
    }

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }

    public int getTokenTtl() {
        return tokenTtl;
    }

    public void setTokenTtl(int tokenTtl) {
        this.tokenTtl = tokenTtl;
    }

    public int getWaitLength() {
        return waitLength;
    }

    public void setWaitLength(int waitLength) {
        this.waitLength = waitLength;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    public String getServiceHost() {
        return serviceHost;
    }

    public void setServiceHost(String serviceHost) {
        this.serviceHost = serviceHost;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

}
