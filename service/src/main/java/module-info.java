module com.networknt.service {
    exports com.networknt.service;

    requires com.networknt.config;
    uses com.networknt.config.Config;
    requires slf4j.api;
    requires java.desktop;
}