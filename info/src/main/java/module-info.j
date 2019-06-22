module com.networknt.info {
    exports com.networknt.info;

    requires com.networknt.config;
    requires com.networknt.handler;
    requires com.networknt.security;
    requires com.networknt.status;
    requires com.networknt.utility;

    requires undertow.core;
    requires org.slf4j;
}