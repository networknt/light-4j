module com.networknt.audit {
    exports com.networknt.audit;

    requires com.networknt.config;
    requires com.networknt.handler;
    requires com.networknt.utility;
    requires com.networknt.mask;

    requires com.fasterxml.jackson.core;
    requires org.slf4j;
    requires java.logging;
}