module com.networknt.cors {
    exports com.networknt.cors;

    requires com.networknt.config;
    requires com.networknt.handler;
    requires com.networknt.utility;

    requires undertow.core;
    requires org.slf4j;
    requires java.logging;
}