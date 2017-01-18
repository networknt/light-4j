package com.networknt.service;

import java.util.Map;

/**
 * Created by stevehu on 2017-01-17.
 */
public interface L {
    String getProtocol();
    void setProtocol(String protocol);

    String getHost();
    void setHost(String host);

    int getPort();
    void setPort(int port);

    Map<String, String> getParameters();
    void setParameters(Map<String, String> map);
}
