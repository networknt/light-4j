package com.networknt.sse;

import com.networknt.config.schema.IntegerField;
import com.networknt.config.schema.StringField;

public class PathPrefix {
    @StringField(
            configFieldName = "pathPrefix",
            pattern = "^/.*"
    )
    private String pathPrefix;

    @IntegerField(
            configFieldName = "keepAliveInterval"
    )
    private int keepAliveInterval;

    public String getPathPrefix() {
        return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    public int getKeepAliveInterval() {
        return keepAliveInterval;
    }

    public void setKeepAliveInterval(int keepAliveInterval) {
        this.keepAliveInterval = keepAliveInterval;
    }
}
