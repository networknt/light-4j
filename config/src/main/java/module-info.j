module com.networknt.config {

    requires com.networknt.decryptor;

    requires com.fasterxml.jackson.annotation;
    requires tools.jackson.core;
    requires tools.jackson.databind;
    requires tools.jackson.datatype.jsr310;
    requires encoder;
    requires org.slf4j;
    requires snakeyaml;

    uses com.networknt.config.Config;
    exports com.networknt.config;
}
