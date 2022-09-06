module com.networknt.rule-loader {
    exports com.networknt.rule;

    requires com.networknt.config;
    requires com.networknt.client;
    requires com.networknt.server;

    requires com.fasterxml.jackson.core;
    requires org.slf4j;
    requires java.logging;
}