module com.networknt.cluster {
    exports com.networknt.cluster;

    requires com.networknt.balance;
    requires com.networknt.registry;
    requires com.networknt.service;
    requires com.networknt.utility;

    requires org.slf4j;
}