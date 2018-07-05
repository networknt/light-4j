package com.networknt.handler.sample;

import com.networknt.handler.LightHttpHandler;
import io.undertow.server.HttpServerExchange;

/**
 * @author Nicholas Azar
 */
public class SampleHttpHandler2 implements LightHttpHandler {

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) {
        System.out.println("Sample Http Handler 2");
    }
}
