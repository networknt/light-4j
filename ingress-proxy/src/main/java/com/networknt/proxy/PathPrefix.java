package com.networknt.proxy;

import com.networknt.config.schema.IntegerField;
import com.networknt.config.schema.StringField;

public class PathPrefix {
    @StringField(
            configFieldName = "pathPrefix",
            pattern = "^/.*"
    )
    String pathPrefix;

    @StringField(
            configFieldName = "host"
    )
    String host;

    @IntegerField(
            configFieldName = "timeout"
    )
    int timeout;

    public String getPathPrefix() {
        return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
