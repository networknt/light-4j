open module client {
    requires com.networknt.common;
    requires com.networknt.config;
    requires com.networknt.status;
    requires com.networknt.utility;
    requires com.networknt.http.string;

    requires java.sql;

    requires com.fasterxml.jackson.annotation;

    requires undertow.core;
    requires slf4j.api;
    requires encoder;
    requires org.apache.commons.codec;
    requires xnio.api;
    requires jboss.threads;
}