module com.networknt.logger.config {
    exports com.networknt.logging.handler;
    exports com.networknt.logging.model;

    requires com.networknt.config;
    requires com.networknt.handler;
    requires com.networknt.utility;
    requires com.networknt.http.string;
    requires com.networknt.body;
    requires com.networknt.exception;

    requires undertow.core;
    requires xnio.api;
    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires com.fasterxml.jackson.core;
}