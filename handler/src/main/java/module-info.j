module com.networknt.handler {
    exports com.networknt.handler;
    exports com.networknt.handler.config;
    requires com.networknt.status;
    requires com.networknt.utility;
    requires com.networknt.config;
    requires com.networknt.service;

    requires undertow.core;
    requires org.slf4j;
}