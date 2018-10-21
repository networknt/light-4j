module com.networknt.content {
    exports com.networknt.content;

    requires com.networknt.config;
    requires com.networknt.handler;
    requires com.networknt.utility;

    requires undertow.core;
    requires java.logging;
}