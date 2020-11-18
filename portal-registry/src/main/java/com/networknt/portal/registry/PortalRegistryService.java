package com.networknt.portal.registry;

import com.networknt.config.Config;

import java.util.List;
import java.util.stream.Collectors;

import static com.networknt.portal.registry.PortalRegistryConfig.CONFIG_NAME;

public class PortalRegistryService {
    static PortalRegistryConfig config = (PortalRegistryConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, PortalRegistryConfig.class);

    private String serviceId;

    private String name;

    private String tag;

    private String protocol;

    private String address;

    private int port;

    private String checkString;

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getCheckString() {
        return checkString;
    }

    public void setCheckString(String checkString) {
        this.checkString = checkString;
    }

    public PortalRegistryService() {
        if(config.httpCheck) {
            checkString = ",\"check\":{\"id\":\"%1$s:%2$s:%3$s:%4$s\",\"deregisterCriticalServiceAfter\":" + config.deregisterAfter + ",\"http\":\"" + "%2$s://%3$s:%4$s/health/%5$s" + "\",\"tlsSkipVerify\":true,\"interval\":" + config.checkInterval + "}}";
        } else {
            checkString = ",\"check\":{\"id\":\"%1$s:%2$s:%3$s:%4$s\",\"deregisterCriticalServiceAfter\":" + config.deregisterAfter + ",\"interval\":" + config.checkInterval + "}}";
        }
    }

    /**
     * Construct a register json payload. Note that deregister internal minimum is 1m.
     *
     * @return String
     */
    @Override
    public String toString() {
        String key = tag == null ? serviceId : serviceId + "|" + tag;
        return "{\"serviceId\":\"" + serviceId +
                "\",\"name\":\"" + name
                + (tag != null ? "\",\"tag\":\"" + tag : "")
                + "\",\"protocol\":\"" + protocol
                + "\",\"address\":\"" + address
                + "\",\"port\":" + port
                + String.format(checkString, key, protocol, address, port, serviceId);
    }

}
