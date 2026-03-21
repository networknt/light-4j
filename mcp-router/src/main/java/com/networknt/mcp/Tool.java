package com.networknt.mcp;

import com.networknt.config.schema.StringField;

public class Tool {
    @StringField(
            configFieldName = "apiType",
            description = "API type (openapi, graphql, hybrid, mcp)"
    )
    String apiType;

    @StringField(
            configFieldName = "endpoint",
            description = "Tool endpoint with method"
    )
    String endpoint;

    @StringField(
            configFieldName = "name",
            description = "Tool name"
    )
    String name;

    @StringField(
            configFieldName = "description",
            description = "Tool description"
    )
    String description;

    @StringField(
            configFieldName = "host",
            description = "Tool host"
    )
    String host;

    @StringField(
            configFieldName = "path",
            description = "Tool path"
    )
    String path;

    @StringField(
            configFieldName = "method",
            description = "Tool method"
    )
    String method;

    @StringField(
            configFieldName = "inputSchema",
            description = "Tool input schema"
    )
    String inputSchema;

    @StringField(
            configFieldName = "protocol",
            description = "Backend protocol (http or mcp)",
            defaultValue = "http"
    )
    String protocol;

    @StringField(
            configFieldName = "serviceId",
            description = "Service ID for discovery"
    )
    String serviceId;

    @StringField(
            configFieldName = "envTag",
            description = "Environment tag for discovery"
    )
    String envTag;

    @StringField(
            configFieldName = "targetHost",
            description = "Target host for discovery"
    )
    String targetHost;

    @StringField(
            configFieldName = "toolMetadata",
            description = "Tool metadata"
    )
    String toolMetadata;

    public String getApiType() {
        return apiType;
    }

    public void setApiType(String apiType) {
        this.apiType = apiType;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getInputSchema() {
        return inputSchema;
    }

    public void setInputSchema(String inputSchema) {
        this.inputSchema = inputSchema;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getEnvTag() {
        return envTag;
    }

    public void setEnvTag(String envTag) {
        this.envTag = envTag;
    }

    public String getTargetHost() {
        return targetHost;
    }

    public void setTargetHost(String targetHost) {
        this.targetHost = targetHost;
    }

    public String getToolMetadata() {
        return toolMetadata;
    }

    public void setToolMetadata(String toolMetadata) {
        this.toolMetadata = toolMetadata;
    }
}
