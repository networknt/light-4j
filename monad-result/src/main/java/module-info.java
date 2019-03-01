module com.networknt.monadresult {
    exports com.networknt.monad;

    requires com.networknt.status;
    requires com.networknt.config;
    requires com.networknt.utility;

    requires java.management;
    requires slf4j.api;
    requires jdk.unsupported;
}