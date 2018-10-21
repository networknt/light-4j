module com.networknt.sanitizer {
    exports com.networknt.sanitizer;

    requires com.networknt.body;
    requires com.networknt.config;
    requires com.networknt.handler;
    requires com.networknt.utility;

    requires undertow.core;
    requires encoder;
    requires slf4j.api;
    requires com.fasterxml.jackson.annotation;
}