package com.networknt.service;

import java.util.Map;

/**
 * Created by stevehu on 2017-01-17.
 */
public class LImpl implements L {
    String protocol;
    String host;
    int port;
    Map<String, String> parameters;

    public LImpl() {
    }

    public LImpl(String protocol, String host, int port, Map<String, String> parameters) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.parameters = parameters;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public Map<String, String> getParameters() {
        return parameters;
    }

    @Override
    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

}
