package com.networknt.sanitizer.builder;

import com.networknt.body.BodyHandler;
import com.networknt.config.Config;
import io.undertow.Handlers;
import io.undertow.server.RoutingHandler;
import io.undertow.util.HeaderMap;
import io.undertow.util.Methods;

import java.util.Deque;
import java.util.Map;

public class HandlerBuilder {

    static RoutingHandler build() {
        return Handlers.routing()
                .add(Methods.GET, "/parameter", exchange -> {
                    Map<String, Deque<String>> parameter = exchange.getQueryParameters();
                    if(parameter != null) {
                        exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(parameter));
                    }
                })
                .add(Methods.GET, "/header", exchange -> {
                    HeaderMap headerMap = exchange.getRequestHeaders();
                    if(headerMap != null) {
                        exchange.getResponseSender().send(headerMap.toString());
                    }
                })
                .add(Methods.POST, "/body", exchange -> {
                    Object body = exchange.getAttachment(BodyHandler.REQUEST_BODY);
                    if(body != null) {
                        exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(body));
                    }
                })
                .add(Methods.POST, "/header", exchange -> {
                    HeaderMap headerMap = exchange.getRequestHeaders();
                    if(headerMap != null) {
                        exchange.getResponseSender().send(headerMap.toString());
                    }
                });
    }
}
