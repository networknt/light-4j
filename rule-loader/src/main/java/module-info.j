module com.networknt.rule-loader {
    exports com.networknt.rule;

    requires com.networknt.config;

    requires com.fasterxml.jackson.core;
    requires org.jose4j;
    requires org.slf4j;
    requires java.logging;
}
