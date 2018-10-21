module com.networknt.traceability {
    exports com.networknt.traceability;

    requires com.networknt.config;
    requires com.networknt.handler;
    requires com.networknt.http.string;
    requires com.networknt.utility;

    requires undertow.core;
    requires slf4j.api;
    requires java.logging;
}