module com.networknt.security {
    exports com.networknt.security;

    requires com.networknt.config;
    requires com.networknt.handler;
    requires com.networknt.utility;
    requires com.networknt.common;
    requires com.networknt.exception;
    requires com.networknt.client;
    requires com.networknt.status;

    requires undertow.core;
    requires slf4j.api;
    requires jose4j;
    requires com.github.benmanes.caffeine;
}