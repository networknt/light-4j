module com.networknt.body {
    exports com.networknt.body;

    requires com.networknt.config;
    requires com.networknt.handler;
    requires com.networknt.utility;

    requires undertow.core;
    requires slf4j.api;
    requires java.logging;
}