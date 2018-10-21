module com.networknt.header {
    exports com.networknt.header;

    requires com.networknt.config;
    requires com.networknt.handler;
    requires com.networknt.utility;

    requires undertow.core;
    requires slf4j.api;
}