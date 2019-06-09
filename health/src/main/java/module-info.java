module com.networknt.health {
    exports com.networknt.health;

    requires com.networknt.handler;

    requires undertow.core;
    requires org.slf4j;
    requires java.logging;
}