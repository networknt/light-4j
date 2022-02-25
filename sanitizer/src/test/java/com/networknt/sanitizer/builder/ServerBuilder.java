package com.networknt.sanitizer.builder;

import com.networknt.body.BodyHandler;
import com.networknt.sanitizer.SanitizerConfig;
import com.networknt.sanitizer.SanitizerHandler;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;

public class ServerBuilder {

    private static ServerBuilder instance;
    private String configName = SanitizerConfig.CONFIG_NAME;

    public static ServerBuilder newServer() {
        if (instance == null) instance = new ServerBuilder();
        return instance;
    }

    public Undertow build() {
        HttpHandler handler = HandlerBuilder.build();

        SanitizerHandler sanitizerHandler = new SanitizerHandler(configName);
        sanitizerHandler.setNext(handler);
        handler = sanitizerHandler;

        BodyHandler bodyHandler = new BodyHandler();
        bodyHandler.setNext(handler);
        handler = bodyHandler;

        instance = null;

        return Undertow.builder()
                .addHttpListener(7080, "localhost")
                .setHandler(handler)
                .build();
    }

    public ServerBuilder withConfigName(String configName) {
        this.configName = configName;
        return this;
    }
}
