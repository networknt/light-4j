module com.networknt.basic.auth {
    exports com.networknt.basicauth;

    requires com.networknt.config;
    requires com.networknt.handler;
    requires com.networknt.status;
    requires com.networknt.utility;
    requires com.networknt.common;

    requires undertow.core;
    requires org.slf4j;
    requires java.logging;
}