module com.networknt.rate.limit {
    exports com.networknt.limit;

    requires com.networknt.config;
    requires com.networknt.handler;
    requires com.networknt.utility;

    requires undertow.core;
    requires java.logging;
}