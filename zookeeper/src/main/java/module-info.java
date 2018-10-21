module com.networknt.zookeeper {
    exports com.networknt.zookeeper;

    requires com.networknt.registry;
    requires com.networknt.status;
    requires com.networknt.utility;
    requires com.networknt.exception;

    requires zkclient;
    requires slf4j.api;
    requires zookeeper;
}