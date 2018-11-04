package com.networknt.resource;

import java.util.List;

public class VirtualHostConfig {
    public static final String CONFIG_NAME = "virtual-host";

    List<VirtualHost> hosts;

    public VirtualHostConfig() {
    }

    public List<VirtualHost> getHosts() {
        return hosts;
    }

    public void setHosts(List<VirtualHost> hosts) {
        this.hosts = hosts;
    }
}
