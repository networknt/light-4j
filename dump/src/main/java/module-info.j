module com.networknt.dump {
    exports com.networknt.dump;

    requires com.networknt.config;
    requires com.networknt.handler;
    requires com.networknt.utility;
    requires com.networknt.status;
    requires com.networknt.mask;
    requires com.networknt.body;

    requires xnio.api;
    requires org.slf4j;
    requires java.logging;
}