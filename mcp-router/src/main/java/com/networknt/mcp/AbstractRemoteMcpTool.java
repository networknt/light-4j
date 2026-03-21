package com.networknt.mcp;

import com.networknt.cluster.Cluster;
import com.networknt.service.SingletonServiceFactory;

abstract class AbstractRemoteMcpTool implements McpTool {
    private static final Cluster cluster = SingletonServiceFactory.getBean(Cluster.class);
    private static final String DEFAULT_INPUT_SCHEMA = "{\"type\": \"object\"}";

    protected final String name;
    protected final String description;
    protected final String endpoint;
    protected final String path;
    protected final String method;
    protected final String inputSchema;
    protected final String protocol;
    protected final String serviceId;
    protected final String envTag;
    protected final String targetHost;

    protected AbstractRemoteMcpTool(String name, String description, String endpoint, String path, String method,
                                    String inputSchema, String protocol, String serviceId, String envTag, String targetHost) {
        this.name = name;
        this.description = description;
        this.endpoint = endpoint;
        this.path = path;
        this.method = method;
        this.inputSchema = inputSchema;
        this.protocol = protocol;
        this.serviceId = serviceId;
        this.envTag = envTag;
        this.targetHost = targetHost;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getEndpoint() {
        return endpoint;
    }

    @Override
    public String getInputSchema() {
        return inputSchema != null ? inputSchema : DEFAULT_INPUT_SCHEMA;
    }

    protected String resolveTargetUrl() {
        if (targetHost != null && !targetHost.isBlank()) {
            return targetHost;
        }
        if (serviceId != null && !serviceId.isBlank()) {
            if (cluster == null) {
                throw new RuntimeException("Cluster service is not available for serviceId-based resolution for tool " + name);
            }
            String url = cluster.serviceToUrl(protocol, serviceId, envTag, null);
            if (url == null || url.isBlank()) {
                throw new RuntimeException("Unable to resolve serviceId " + serviceId + " for tool " + name);
            }
            return url;
        }
        throw new RuntimeException("No targetHost or serviceId provided for tool " + name);
    }

    protected String buildHostHeader(java.net.URI uri) {
        String hostHeader = uri.getHost();
        int port = uri.getPort();
        if (port != -1 && port != 80 && port != 443) {
            hostHeader += ":" + port;
        }
        return hostHeader;
    }
}
