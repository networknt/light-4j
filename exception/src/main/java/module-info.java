module com.networknt.exception {
    exports com.networknt.exception;

    requires com.networknt.config;
    requires com.networknt.handler;
    requires com.networknt.status;
    requires com.networknt.utility;

    requires undertow.core;
    requires slf4j.api;
    requires com.fasterxml.jackson.annotation;

    requires java.logging;
}