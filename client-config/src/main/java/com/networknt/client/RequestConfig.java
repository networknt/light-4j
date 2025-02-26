package com.networknt.client;

import com.networknt.config.Config;
import com.networknt.config.MapLoadable;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.IntegerField;

import java.util.Map;

public class RequestConfig /*implements MapLoadable*/ {

    public static final String ERROR_THRESHOLD = "errorThreshold";
    public static final String TIMEOUT = "timeout";
    public static final String RESET_TIMEOUT = "resetTimeout";
    public static final String INJECT_OPEN_TRACING = "injectOpenTracing";
    public static final String INJECT_CALLER_ID = "injectCallerId";
    public static final String ENABLE_HTTP_2 = "enableHttp2";
    public static final String CONNECTION_POOL_SIZE = "connectionPoolSize";
    public static final String CONNECTION_EXPIRE_TIME = "connectionExpireTime";
    public static final String MAX_REQ_PER_CONN = "maxReqPerConn";
    public static final String MAX_CONNECTION_NUM_PER_HOST = "maxConnectionNumPerHost";
    public static final String MIN_CONNECTION_NUM_PER_HOST = "minConnectionNumPerHost";
    public static final String MAX_REQUEST_RETRY = "maxRequestRetry";
    public static final String REQUEST_RETRY_DELAY = "requestRetryDelay";
    @IntegerField(
            configFieldName = ERROR_THRESHOLD,
            externalizedKeyName = ERROR_THRESHOLD,
            externalized = true,
            defaultValue = 2,
            description = "number of timeouts/errors to break the circuit"
    )
    private int errorThreshold;

    @IntegerField(
            configFieldName = TIMEOUT,
            externalizedKeyName = TIMEOUT,
            externalized = true,
            defaultValue = 3000,
            description = "timeout in millisecond to indicate a client error. " +
                    "If light-4j Http2Client is used, it is the timeout to get the\n" +
                    "connection. If http-client (JDK 11 client wrapper) is used, it is the request timeout."
    )
    private int timeout;

    @IntegerField(
            configFieldName = RESET_TIMEOUT,
            externalizedKeyName = RESET_TIMEOUT,
            externalized = true,
            defaultValue = 7000,
            description = "reset the circuit after this timeout in millisecond"
    )
    private int resetTimeout;

    @BooleanField(
            configFieldName = INJECT_OPEN_TRACING,
            externalizedKeyName = INJECT_OPEN_TRACING,
            externalized = true,
            description = "if open tracing is enabled. traceability, " +
                    "correlation and metrics should not be in the chain if opentracing is used."
    )
    private boolean injectOpenTracing;

    @BooleanField(
            configFieldName = INJECT_CALLER_ID,
            externalizedKeyName = INJECT_CALLER_ID,
            externalized = true,
            description = "inject serviceId as callerId into the http header for metrics to collect the caller. " +
                    "The serviceId is from server.yml"
    )
    private boolean injectCallerId;

    @BooleanField(
            configFieldName = ENABLE_HTTP_2,
            externalizedKeyName = ENABLE_HTTP_2,
            externalized = true,
            defaultValue = true,
            description = "the flag to indicate whether http/2 is enabled when calling client.callService()"
    )
    private boolean enableHttp2;

    @IntegerField(
            configFieldName = CONNECTION_POOL_SIZE,
            externalizedKeyName = CONNECTION_POOL_SIZE,
            externalized = true,
            defaultValue = 1000,
            description = "the maximum host capacity of connection pool"
    )
    private int connectionPoolSize;

    @IntegerField(
            configFieldName = CONNECTION_EXPIRE_TIME,
            externalizedKeyName = CONNECTION_EXPIRE_TIME,
            externalized = true,
            defaultValue = 1800000,
            description = "Connection expire time when connection pool is used. " +
                    "By default, the cached connection will be closed after 30 minutes.\n" +
                    "This is one way to force the connection to be closed so that " +
                    "the client-side discovery can be balanced."
    )
    private int connectionExpireTime;

    @IntegerField(
            configFieldName = MAX_REQ_PER_CONN,
            externalizedKeyName = MAX_REQ_PER_CONN,
            externalized = true,
            defaultValue = 1000000,
            description = "The maximum request limitation for each connection in the connection pool. " +
                    "By default, a connection will be closed after\n" +
                    "sending 1 million requests. " +
                    "This is one way to force the client-side discovery to re-balance the connections."
    )
    private int maxReqPerConn;

    @IntegerField(
            configFieldName = MAX_CONNECTION_NUM_PER_HOST,
            externalizedKeyName = MAX_CONNECTION_NUM_PER_HOST,
            externalized = true,
            defaultValue = 1000,
            description = "maximum quantity of connection in connection pool for each host"
    )
    private int maxConnectionNumPerHost;

    @IntegerField(
            configFieldName = MIN_CONNECTION_NUM_PER_HOST,
            externalizedKeyName = MIN_CONNECTION_NUM_PER_HOST,
            externalized = true,
            defaultValue = 250,
            description = "minimum quantity of connection in connection pool for each host. " +
                    "The corresponding connection number will shrink to minConnectionNumPerHost\n" +
                    "by remove least recently used connections when the connection number " +
                    "of a host reach 0.75 * maxConnectionNumPerHost."
    )
    private int minConnectionNumPerHost;

    @IntegerField(
            configFieldName = MAX_REQUEST_RETRY,
            externalizedKeyName = MAX_REQUEST_RETRY,
            externalized = true,
            defaultValue = 3,
            description = "Maximum request retry times for each request. If you don't want to retry, set it to 1."
    )
    private int maxRequestRetry;

    @IntegerField(
            configFieldName = REQUEST_RETRY_DELAY,
            externalizedKeyName = REQUEST_RETRY_DELAY,
            externalized = true,
            defaultValue = 1000,
            description = "The delay time in milliseconds for each request retry."
    )
    private int requestRetryDelay;

    public int getErrorThreshold() {
        return errorThreshold;
    }

    public int getTimeout() {
        return timeout;
    }

    public int getResetTimeout() {
        return resetTimeout;
    }

    public boolean isInjectOpenTracing() {
        return injectOpenTracing;
    }

    public boolean isInjectCallerId() {
        return injectCallerId;
    }

    public boolean isEnableHttp2() {
        return enableHttp2;
    }


    public void setIsEnableHttp2(boolean enableHttp2) {
        this.enableHttp2 = enableHttp2;
    }

    public int getConnectionPoolSize() {
        return connectionPoolSize;
    }

    public int getConnectionExpireTime() {
        return connectionExpireTime;
    }

    public int getMaxReqPerConn() {
        return maxReqPerConn;
    }

    public int getMaxConnectionNumPerHost() {
        return maxConnectionNumPerHost;
    }

    public int getMinConnectionNumPerHost() {
        return minConnectionNumPerHost;
    }

    public int getMaxRequestRetry() {
        return maxRequestRetry;
    }

    public int getRequestRetryDelay() {
        return requestRetryDelay;
    }

//    @Override
//    public void loadData(Map<String, Object> data) {
//        if (data.containsKey(ERROR_THRESHOLD)) {
//            errorThreshold = Config.loadIntegerValue(ERROR_THRESHOLD, data.get(ERROR_THRESHOLD));
//        }
//
//        if (data.containsKey(TIMEOUT)) {
//            timeout = Config.loadIntegerValue(TIMEOUT, data.get(TIMEOUT));
//        }
//
//        if (data.containsKey(RESET_TIMEOUT)) {
//            resetTimeout = Config.loadIntegerValue(RESET_TIMEOUT, data.get(RESET_TIMEOUT));
//        }
//
//        if (data.containsKey(INJECT_OPEN_TRACING)) {
//            injectOpenTracing = Config.loadBooleanValue(INJECT_OPEN_TRACING, data.get(INJECT_OPEN_TRACING));
//        }
//
//        if (data.containsKey(INJECT_CALLER_ID)) {
//            injectCallerId = Config.loadBooleanValue(INJECT_CALLER_ID, data.get(INJECT_CALLER_ID));
//        }
//
//        if (data.containsKey(ENABLE_HTTP_2)) {
//            enableHttp2 = Config.loadBooleanValue(ENABLE_HTTP_2, data.get(ENABLE_HTTP_2));
//        }
//
//        if (data.containsKey(CONNECTION_POOL_SIZE)) {
//            connectionPoolSize = Config.loadIntegerValue(CONNECTION_POOL_SIZE, data.get(CONNECTION_POOL_SIZE));
//        }
//
//        if (data.containsKey(CONNECTION_EXPIRE_TIME)) {
//            connectionExpireTime = Config.loadIntegerValue(CONNECTION_EXPIRE_TIME, data.get(CONNECTION_EXPIRE_TIME));
//        }
//
//        if (data.containsKey(MAX_REQ_PER_CONN)) {
//            maxReqPerConn = Config.loadIntegerValue(MAX_REQ_PER_CONN, data.get(MAX_REQ_PER_CONN));
//        }
//
//        if (data.containsKey(MAX_CONNECTION_NUM_PER_HOST)) {
//            maxConnectionNumPerHost = Config.loadIntegerValue(MAX_CONNECTION_NUM_PER_HOST, data.get(MAX_CONNECTION_NUM_PER_HOST));
//        }
//
//        if (data.containsKey(MIN_CONNECTION_NUM_PER_HOST)) {
//            minConnectionNumPerHost = Config.loadIntegerValue(MIN_CONNECTION_NUM_PER_HOST, data.get(MIN_CONNECTION_NUM_PER_HOST));
//        }
//
//        if (data.containsKey(MAX_REQUEST_RETRY)) {
//            maxRequestRetry = Config.loadIntegerValue(MAX_REQUEST_RETRY, data.get(MAX_REQUEST_RETRY));
//        }
//
//        if (data.containsKey(REQUEST_RETRY_DELAY)) {
//            requestRetryDelay = Config.loadIntegerValue(REQUEST_RETRY_DELAY, data.get(REQUEST_RETRY_DELAY));
//        }
//    }
}
