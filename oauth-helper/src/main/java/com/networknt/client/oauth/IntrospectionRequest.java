package com.networknt.client.oauth;

/**
 * This is the generic introspection request for simple web token. The static serverUrl will be used if
 * available. Otherwise, the serviceId will be used to look up the token service. In the client.yml file
 * we are using the token key service for the introspection configuration as they are mutually exclusive.
 *
 * @author Steve Hu
 *
 */
public class IntrospectionRequest {
    protected String serverUrl;
    protected String proxyHost;
    protected int proxyPort;
    protected String serviceId;
    protected String uri;
    protected String clientId;
    protected String clientSecret;
    protected boolean enableHttp2;
    protected String swt;

    public IntrospectionRequest(String swt) {
        this.swt = swt;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
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
        this.clientSecret = clientSecret;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public boolean isEnableHttp2() { return enableHttp2; }

    public void setEnableHttp2(boolean enableHttp2) { this.enableHttp2 = enableHttp2; }

    public String getSwt() {
        return swt;
    }

    public void setSwt(String swt) {
        this.swt = swt;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    @Override
    public String toString() {
        return "IntrospectionRequest{" +
                "serverUrl='" + serverUrl + '\'' +
                ", proxyHost='" + proxyHost + '\'' +
                ", proxyPort=" + proxyPort +
                ", serviceId='" + serviceId + '\'' +
                ", uri='" + uri + '\'' +
                ", clientId='" + clientId + '\'' +
                ", clientSecret='" + clientSecret + '\'' +
                ", enableHttp2=" + enableHttp2 +
                ", swt='" + swt + '\'' +
                '}';
    }
}
