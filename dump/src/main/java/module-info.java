module com.networknt.dump {
    exports com.networknt.dump;

    requires com.networknt.config;
    requires com.networknt.handler;
    requires com.networknt.utility;
    requires com.networknt.status;

    requires xnio.api;
    requires java.logging;
}