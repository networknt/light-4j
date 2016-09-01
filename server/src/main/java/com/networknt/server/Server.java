package com.networknt.server;

import com.networknt.config.Config;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathTemplateHandler;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.Options;

import java.util.EnumSet;
import java.util.ServiceLoader;
import java.util.Set;


public class Server {

    static final Logger logger = LoggerFactory.getLogger(Server.class);

    static protected boolean shutdownRequested = false;
    static Undertow server = null;
    static String configName = "server";
    public static PathTemplateHandler handler;

    public static void main(final String[] args) {
        logger.info("server starts");
        // init JsonPath
        configJsonPath();
        // init handler
        initHandler();
        // add shutdown hook here.
        addDaemonShutdownHook();
        start();
    }

    static public void start() {

        ServerConfig config = (ServerConfig) Config.getInstance().getJsonObjectConfig(configName, ServerConfig.class);

        HttpHandler handler = null;

        final ServiceLoader<HandlerProvider> handlerLoaders = ServiceLoader.load(HandlerProvider.class);
        for (final HandlerProvider provider : handlerLoaders) {
            if(provider.getHandler() instanceof HttpHandler) {
                handler = provider.getHandler();
                break;
            }
        }

        Undertow.builder()
                .addHttpListener(
                        config.getPort(),
                        config.getIp())
                .setBufferSize(1024 * 16)
                .setIoThreads(Runtime.getRuntime().availableProcessors() * 2) //this seems slightly faster in some configurations
                .setSocketOption(Options.BACKLOG, 10000)
                .setServerOption(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, false) //don't send a keep-alive header for HTTP/1.1 requests, as it is not required
                .setServerOption(UndertowOptions.ALWAYS_SET_DATE, true)
                .setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, false)
                .setHandler(Handlers.header(handler,
                        Headers.SERVER_STRING, "Undertow"))
                .setWorkerThreads(200)
                .build()
                .start();

    }

    static public void stop() {
        if(server != null) server.stop();
    }

    // implement shutdown hook here.
    static public void shutdown() {
        stop();
        logger.info("Cleaning up before server shutdown");
    }

    static protected void addDaemonShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                Server.shutdown();
            }
        });
    }

    static void configJsonPath() {
        Configuration.setDefaults(new Configuration.Defaults() {

            private final JsonProvider jsonProvider = new JacksonJsonProvider();
            private final MappingProvider mappingProvider = new JacksonMappingProvider();

            @Override
            public JsonProvider jsonProvider() {
                return jsonProvider;
            }

            @Override
            public MappingProvider mappingProvider() {
                return mappingProvider;
            }

            @Override
            public Set<Option> options() {
                return EnumSet.noneOf(Option.class);
            }
        });
    }

    static void initHandler() {
        // using reflection to find the
    }
}
