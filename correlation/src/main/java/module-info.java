module com.networknt.correlation {
    exports com.networknt.correlation;

    requires com.networknt.handler;
    requires com.networknt.config;
    requires com.networknt.http.string;
    requires com.networknt.utility;

    requires undertow.core;
    requires slf4j.api;
    requires java.logging;
}