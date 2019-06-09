module com.networknt.consul {
    exports com.networknt.consul;

    requires com.networknt.registry;
    requires com.networknt.utility;
    requires com.networknt.config;
    requires com.networknt.client;
    requires com.networknt.common;
    requires com.networknt.http.string;

    requires org.slf4j;
    requires undertow.core;

}