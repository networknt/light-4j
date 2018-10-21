module com.networknt.deref.token {
    exports com.networknt.deref;

    requires com.networknt.client;
    requires com.networknt.config;
    requires com.networknt.handler;
    requires com.networknt.utility;

    requires undertow.core;
    requires slf4j.api;
}