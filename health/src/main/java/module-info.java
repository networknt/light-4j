module com.networknt.health {
    exports com.networknt.health;

    requires com.networknt.handler;

    requires undertow.core;
    requires slf4j.api;
}