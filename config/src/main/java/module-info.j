module com.networknt.config {

    requires com.networknt.decryptor;

    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires encoder;
    requires org.slf4j;
    requires snakeyaml;

    uses com.networknt.config.Config;
    exports com.networknt.config;
}