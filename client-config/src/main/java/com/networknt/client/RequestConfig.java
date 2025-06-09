package com.networknt.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.IntegerField;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestConfig {

    @IntegerField(
            configFieldName = ClientConfig.ERROR_THRESHOLD,
            externalizedKeyName = ClientConfig.ERROR_THRESHOLD,
            externalized = true,
            defaultValue = 2,
            description = "number of timeouts/errors to break the circuit"
    )
    @JsonProperty(ClientConfig.ERROR_THRESHOLD)
    private Integer errorThreshold = 2;

    @IntegerField(
            configFieldName = ClientConfig.TIMEOUT,
            externalizedKeyName = ClientConfig.TIMEOUT,
            externalized = true,
            defaultValue = 3000,
            description = "timeout in millisecond to indicate a client error. " +
                    "If light-4j Http2Client is used, it is the timeout to get the\n" +
                    "connection. If http-client (JDK 11 client wrapper) is used, it is the request timeout. The default value is 3000."
    )
    @JsonProperty(ClientConfig.TIMEOUT)
    private Integer timeout = 3000;

    @IntegerField(
            configFieldName = ClientConfig.RESET_TIMEOUT,
            externalizedKeyName = ClientConfig.RESET_TIMEOUT,
            externalized = true,
            defaultValue = 7000,
            description = "reset the circuit after this timeout in millisecond"
    )
    @JsonProperty(ClientConfig.RESET_TIMEOUT)
    private Integer resetTimeout = 7000;

    @BooleanField(
            configFieldName = ClientConfig.INJECT_OPEN_TRACING,
            externalizedKeyName = ClientConfig.INJECT_OPEN_TRACING,
            externalized = true,
            description = "if open tracing is enabled. traceability, " +
                    "correlation and metrics should not be in the chain if opentracing is used."
    )
    @JsonProperty(ClientConfig.INJECT_OPEN_TRACING)
    private Boolean injectOpenTracing = false;

    @BooleanField(
            configFieldName = ClientConfig.INJECT_CALLER_ID,
            externalizedKeyName = ClientConfig.INJECT_CALLER_ID,
            externalized = true,
            description = "inject serviceId as callerId into the http header for metrics to collect the caller. " +
                    "The serviceId is from server.yml"
    )
    @JsonProperty(ClientConfig.INJECT_CALLER_ID)
    private Boolean injectCallerId = false;

    @BooleanField(
            configFieldName = ClientConfig.ENABLE_HTTP_2,
            externalizedKeyName = ClientConfig.ENABLE_HTTP_2,
            externalized = true,
            defaultValue = true,
            description = "the flag to indicate whether http/2 is enabled when calling client.callService()"
    )
    @JsonProperty(ClientConfig.ENABLE_HTTP2)
    private Boolean enableHttp2 = true;

    @IntegerField(
            configFieldName = ClientConfig.CONNECTION_POOL_SIZE,
            externalizedKeyName = ClientConfig.CONNECTION_POOL_SIZE,
            externalized = true,
            defaultValue = 1000,
            description = "the maximum host capacity of connection pool"
    )
    @JsonProperty(ClientConfig.CONNECTION_POOL_SIZE)
    private Integer connectionPoolSize = 1000;

    @IntegerField(
            configFieldName = ClientConfig.CONNECTION_EXPIRE_TIME,
            externalizedKeyName = ClientConfig.CONNECTION_EXPIRE_TIME,
            externalized = true,
            defaultValue = 1800000,
            description = "Connection expire time when connection pool is used. " +
                    "By default, the cached connection will be closed after 30 minutes.\n" +
                    "This is one way to force the connection to be closed so that " +
                    "the client-side discovery can be balanced."
    )
    @JsonProperty(ClientConfig.CONNECTION_EXPIRE_TIME)
    private Integer connectionExpireTime = 1800000; // 30 minutes in milliseconds

    @IntegerField(
            configFieldName = ClientConfig.MAX_REQ_PER_CONN,
            externalizedKeyName = ClientConfig.MAX_REQ_PER_CONN,
            externalized = true,
            defaultValue = 1000000,
            description = "The maximum request limitation for each connection in the connection pool. " +
                    "By default, a connection will be closed after\n" +
                    "sending 1 million requests. " +
                    "This is one way to force the client-side discovery to re-balance the connections."
    )
    @JsonProperty(ClientConfig.MAX_REQ_PER_CONN)
    private Integer maxReqPerConn = 1000000;

    @IntegerField(
            configFieldName = ClientConfig.MAX_CONNECTION_NUM_PER_HOST,
            externalizedKeyName = ClientConfig.MAX_CONNECTION_NUM_PER_HOST,
            externalized = true,
            defaultValue = 1000,
            description = "maximum quantity of connection in connection pool for each host"
    )
    @JsonProperty(ClientConfig.MAX_CONNECTION_NUM_PER_HOST)
    private Integer maxConnectionNumPerHost = 1000;

    @IntegerField(
            configFieldName = ClientConfig.MIN_CONNECTION_NUM_PER_HOST,
            externalizedKeyName = ClientConfig.MIN_CONNECTION_NUM_PER_HOST,
            externalized = true,
            defaultValue = 250,
            description = "minimum quantity of connection in connection pool for each host. " +
                    "The corresponding connection number will shrink to minConnectionNumPerHost\n" +
                    "by remove least recently used connections when the connection number " +
                    "of a host reach 0.75 * maxConnectionNumPerHost."
    )
    @JsonProperty(ClientConfig.MIN_CONNECTION_NUM_PER_HOST)
    private Integer minConnectionNumPerHost = 250;

    @IntegerField(
            configFieldName = ClientConfig.MAX_REQUEST_RETRY,
            externalizedKeyName = ClientConfig.MAX_REQUEST_RETRY,
            externalized = true,
            defaultValue = 3,
            description = "Maximum request retry times for each request. If you don't want to retry, set it to 1. The default value is 3."
    )
    @JsonProperty(ClientConfig.MAX_REQUEST_RETRY)
    private Integer maxRequestRetry = 3;

    @IntegerField(
            configFieldName = ClientConfig.REQUEST_RETRY_DELAY,
            externalizedKeyName = ClientConfig.REQUEST_RETRY_DELAY,
            externalized = true,
            defaultValue = 1000,
            description = "The delay time in milliseconds for each request retry. The default value is 1000."
    )
    @JsonProperty(ClientConfig.REQUEST_RETRY_DELAY)
    private Integer requestRetryDelay = 1000;

    public Integer getErrorThreshold() {
        return errorThreshold;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public Integer getResetTimeout() {
        return resetTimeout;
    }

    public Boolean isInjectOpenTracing() {
        return injectOpenTracing;
    }

    public Boolean isInjectCallerId() {
        return injectCallerId;
    }

    public Boolean isEnableHttp2() {
        return enableHttp2;
    }

    public void setIsEnableHttp2(Boolean enableHttp2) {
        this.enableHttp2 = enableHttp2;
    }

    public Integer getConnectionPoolSize() {
        return connectionPoolSize;
    }

    public Integer getConnectionExpireTime() {
        return connectionExpireTime;
    }

    public Integer getMaxReqPerConn() {
        return maxReqPerConn;
    }

    public Integer getMaxConnectionNumPerHost() {
        return maxConnectionNumPerHost;
    }

    public Integer getMinConnectionNumPerHost() {
        return minConnectionNumPerHost;
    }

    public Integer getMaxRequestRetry() {
        return maxRequestRetry;
    }

    public Integer getRequestRetryDelay() {
        return requestRetryDelay;
    }
}
