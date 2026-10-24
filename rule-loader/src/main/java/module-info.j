module com.networknt.rule-loader {
    exports com.networknt.rule;

    requires com.networknt.config;

    requires tools.jackson.core;
    requires org.jose4j;
    requires org.slf4j;
    requires java.logging;
}
