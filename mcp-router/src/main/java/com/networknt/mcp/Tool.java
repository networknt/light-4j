package com.networknt.mcp;

import com.networknt.config.schema.StringField;

public class Tool {
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
}
