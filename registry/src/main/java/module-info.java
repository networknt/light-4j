module com.networknt.registry {
    exports com.networknt.registry;
    exports com.networknt.registry.support;
    exports com.networknt.registry.support.command;

    requires com.networknt.exception;
    requires com.networknt.status;
    requires com.networknt.utility;
    requires com.networknt.switcher;

    requires slf4j.api;

}