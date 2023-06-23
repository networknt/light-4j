package com.networknt.proxy;

import com.networknt.config.ConfigInjection;

/**
 * This is an object that contains all the authentication info for each path prefix in the pathPrefixAuth config
 * section. By making it a list of objects, we can support unlimited number of APIs with different authentication
 * parameters.
 */
public class PathPrefixAuth {
    String grantType;
    String pathPrefix;
    String authIssuer;
    String authSubject;
    String authAudience;
    String iv;
    int tokenTtl;
    int waitLength;
    String tokenUrl;
    String serviceHost;

    String username;
    String password;
    String clientId;
    String scope;
    String clientSecret;
    String responseType;

    long expiration;
    String accessToken;

    public PathPrefixAuth() {
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = (String) ConfigInjection.decryptEnvValue(ConfigInjection.getDecryptor(), password);
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = (String) ConfigInjection.decryptEnvValue(ConfigInjection.getDecryptor(), clientSecret);
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
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

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}
