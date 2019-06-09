module com.networknt.exception {
    exports com.networknt.exception;

    requires com.networknt.config;
    requires com.networknt.handler;
    requires com.networknt.status;
    requires com.networknt.utility;

    requires undertow.core;
    requires org.slf4j;
    requires com.fasterxml.jackson.annotation;

    requires java.logging;
}