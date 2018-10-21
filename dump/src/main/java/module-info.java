module com.networknt.dump {
    exports com.networknt.dump;

    requires com.networknt.config;
    requires com.networknt.handler;
    requires com.networknt.utility;

    requires undertow.core;
    requires slf4j.api;
}