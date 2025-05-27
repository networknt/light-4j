package com.networknt.token.limit;

import com.networknt.handler.LightHttpHandler;
import com.networknt.http.MediaType;
import com.networknt.status.HttpStatus;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.util.UUID;

public class TestHandler implements LightHttpHandler {
    public static int counter = 0;
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        exchange.setStatusCode(HttpStatus.OK.value());
        UUID uuid = UUID.randomUUID();
        String s = "{\"token_type\":\"bearer\",\"expire_in\":\"600\",\"access_token\":\"" + uuid.toString() + "\",\"counter\": %d}";
        s = String.format(s, counter++);
        exchange.getResponseSender().send(s);
    }
}
