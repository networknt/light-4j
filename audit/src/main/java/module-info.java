module com.networknt.audit {
    exports com.networknt.audit;

    requires com.networknt.config;
    requires com.networknt.handler;
    requires com.networknt.utility;

    requires undertow.core;
    requires com.fasterxml.jackson.core;
    requires slf4j.api;
}