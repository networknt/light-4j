package com.networknt.portal.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PortalRegistryService {


    private String serviceId;

    private String name;

    private String tag;

    private String protocol;

    private String address;

    private int port;

    private String version;

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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getInstanceId() {
        String key = tag == null ? serviceId : serviceId + "|" + tag;
        return key + ":" + protocol + ":" + address + ":" + port;
    }

    public List<String> getCapabilities() {
        List<String> capabilities = new ArrayList<>();
        capabilities.add("discovery");
        return capabilities;
    }

    public Map<String, Object> toRegisterParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("instanceId", getInstanceId());
        params.put("serviceId", serviceId);
        if (tag != null) {
            params.put("tag", tag);
            params.put("environment", tag);
        }
        params.put("protocol", protocol);
        params.put("address", address);
        params.put("port", port);
        params.put("version", version);
        params.put("capabilities", getCapabilities());
        return params;
    }

    public Map<String, Object> toControllerRsRegisterParams(String jwt) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("jwt", jwt);
        params.put("serviceId", serviceId);
        if (tag != null) {
            params.put("envTag", tag);
        }
        params.put("environment", tag != null ? tag : "default");
        params.put("version", version != null ? version : "1.0.0");
        params.put("protocol", protocol);
        params.put("port", port);
        params.put("tags", Map.of());
        return params;
    }

    /**
     * Construct a register JSON payload.
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
                + ",\"key\":\"" + key + "\"}";
    }

}
