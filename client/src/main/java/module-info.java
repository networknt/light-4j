open module com.networknt.client {
    exports com.networknt.client;
    exports com.networknt.client.oauth;
//    exports io.undertow.client.http;

    requires com.networknt.common;
    requires com.networknt.config;
    requires com.networknt.status;
    requires com.networknt.utility;
    requires com.networknt.http.string;
    requires com.networknt.monadresult;

    requires java.sql;
    requires com.fasterxml.jackson.annotation;
    requires undertow.core;
    requires slf4j.api;
    requires encoder;
    requires org.apache.commons.codec;
    requires xnio.api;
    requires org.jboss.logging;
}
