module com.networknt.correlation {
    exports com.networknt.correlation;

    requires com.networknt.handler;
    requires com.networknt.config;
    requires com.networknt.http.string;
    requires com.networknt.utility;

    requires undertow.core;
    requires org.slf4j;
    requires java.logging;
}