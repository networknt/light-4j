module com.networknt.ip.whitelist {
    exports com.networknt.whitelist;

    requires com.networknt.config;
    requires com.networknt.handler;
    requires com.networknt.utility;

    requires undertow.core;
    requires xnio.api;
    requires slf4j.api;
}