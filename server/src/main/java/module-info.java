module com.networknt.server {
    exports com.networknt.server;

    requires com.networknt.client;
    requires com.networknt.common;
    requires com.networknt.config;
    requires com.networknt.handler;
    requires com.networknt.registry;
    requires com.networknt.service;
    requires com.networknt.utility;
    requires com.networknt.switcher;

    requires undertow.core;
    requires org.slf4j;
    requires xnio.api;
    requires json.path;
}