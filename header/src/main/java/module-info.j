module com.networknt.header {
    exports com.networknt.header;

    requires com.networknt.config;
    requires com.networknt.handler;
    requires com.networknt.utility;

    requires undertow.core;
    requires org.slf4j;
    requires java.logging;
}