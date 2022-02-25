package com.networknt.sanitizer;

import com.networknt.sanitizer.builder.ServerBuilder;
import io.undertow.Undertow;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SanitizerHandlerWithEncodeType {

    private static final Logger LOGGER = LoggerFactory.getLogger(SanitizerHandlerWithEncodeTest.class);

    private static Undertow server = null;

    @Test(expected = IllegalStateException.class)
    public void testStartServer() {
        if(server == null) {
            LOGGER.info("starting server");
            server = ServerBuilder.newServer().withConfigName("sanitizer_with_encode_type").build();
            server.start();
        }
    }
}
