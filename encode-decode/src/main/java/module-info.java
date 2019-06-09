module com.networknt.encode.decode {
    exports com.networknt.encode;
    exports com.networknt.decode;

    requires com.networknt.config;
    requires com.networknt.handler;
    requires com.networknt.utility;
    requires com.networknt.status;

    requires xnio.api;
    requires org.slf4j;
    requires java.logging;
}