module com.networknt.prometheus {
    exports com.networknt.metrics.prometheus;

    requires com.networknt.audit;
    requires com.networknt.config;
    requires com.networknt.handler;
    requires com.networknt.utility;
    requires com.networknt.client;

    requires simpleclient;
    requires simpleclient.common;
    requires com.fasterxml.jackson.annotation;
    requires undertow.core;
    requires org.slf4j;
}