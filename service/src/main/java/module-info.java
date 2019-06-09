module com.networknt.service {
    exports com.networknt.service;
    requires com.networknt.config;
    uses com.networknt.config.Config;
    requires org.slf4j;
    requires java.desktop;
}
