module com.networknt.zookeeper {
    exports com.networknt.zookeeper;

    requires com.networknt.registry;
    requires com.networknt.status;
    requires com.networknt.utility;

    requires zkclient;
    requires org.slf4j;
    requires zookeeper;
}