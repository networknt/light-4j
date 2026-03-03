package com.networknt.proxy;

import com.networknt.config.schema.IntegerField;
import com.networknt.config.schema.StringField;

/**
 * PathPrefix class
 *
 * @author Steve Hu
 */
public class PathPrefix {
    @StringField(
            configFieldName = "pathPrefix",
            pattern = "^/.*"
    )
    private String pathPrefix;

    @StringField(
            configFieldName = "host"
    )
    private String host;

    @IntegerField(
            configFieldName = "timeout"
    )
    private int timeout;

    /**
     * Default Constructor
     */
    public PathPrefix() {
    }

    /**
     * get path prefix
     * @return String
     */
    public String getPathPrefix() {
        return pathPrefix;
    }

    /**
     * set path prefix
     * @param pathPrefix String
     */
    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    /**
     * get host
     * @return String
     */
    public String getHost() {
        return host;
    }

    /**
     * set host
     * @param host String
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * get timeout
     * @return int
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * set timeout
     * @param timeout int
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
