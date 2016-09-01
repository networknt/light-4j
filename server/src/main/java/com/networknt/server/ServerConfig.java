package com.networknt.server;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ServerConfig {
    String ip;
    int port;
    @JsonIgnore
    String description;

    public ServerConfig() {
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
