/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.server;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import com.networknt.config.Config;
import com.networknt.handler.MiddlewareHandler;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
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

    public static void main(final String[] args) {
        logger.info("server starts");
        start();
    }

    static public void start() {

        // init JsonPath
        configJsonPath();
        // add shutdown hook here.
        addDaemonShutdownHook();

        ServerConfig config = (ServerConfig) Config.getInstance().getJsonObjectConfig(configName, ServerConfig.class);

        HttpHandler handler = null;

        // API routing handler or others handler implemented by application developer.
        final ServiceLoader<HandlerProvider> handlerLoaders = ServiceLoader.load(HandlerProvider.class);
        for (final HandlerProvider provider : handlerLoaders) {
            if (provider.getHandler() instanceof HttpHandler) {
                handler = provider.getHandler();
                break;
            }
        }
        if (handler == null) {
            logger.warn("No route handler provider available in the classpath");
            return;
        }

        // Middleware Handlers plugged into the handler chain.
        final ServiceLoader<MiddlewareHandler> middlewareLoaders = ServiceLoader.load(MiddlewareHandler.class);
        logger.debug("found middlewareLoaders", middlewareLoaders);
        for (final MiddlewareHandler middlewareHandler : middlewareLoaders) {
            logger.info("Plugin: " + middlewareHandler.getClass().getName());
            handler = middlewareHandler.setNext(handler);
        }

        // check if server info handler needs to be installed
        // TODO move to swagger specification
        /*
        ServerInfoConfig serverInfoConfig = (ServerInfoConfig)Config.getInstance().getJsonObjectConfig(ServerInfoHandler.CONFIG_NAME, ServerInfoConfig.class);
        if(serverInfoConfig.isEnableServerInfo()) {
            ServerInfoHelper.inject(handler);
        }
        */

        /*
        // check if validator needs to be installed.
        ValidatorConfig validatorConfig = (ValidatorConfig)Config.getInstance().getJsonObjectConfig(ValidatorHandler.CONFIG_NAME, ValidatorConfig.class);
        if(validatorConfig.isEnableValidator()) {
            ValidatorHandler validatorHandler = new ValidatorHandler(handler);
            handler = validatorHandler;
            ModuleRegistry.registerModule(ValidatorHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(ValidatorHandler.CONFIG_NAME), null);
        }

        // check if simple audit log handler needs to be installed.
        Object object = Config.getInstance().getJsonMapConfig(SimpleAuditHandler.CONFIG_NAME).get(SimpleAuditHandler.ENABLE_SIMPLE_AUDIT);
        if(object != null && (Boolean)object == true) {
            SimpleAuditHandler simpleAuditHandler = new SimpleAuditHandler(handler);
            handler = simpleAuditHandler;
            ModuleRegistry.registerModule(SimpleAuditHandler.class.getName(), SimpleAuditHandler.config, null);
        }

        // check if full audit log handler needs to be installed.
        object = Config.getInstance().getJsonMapConfig(FullAuditHandler.CONFIG_NAME).get(FullAuditHandler.ENABLE_FULL_AUDIT);
        if(object != null && (Boolean)object == true) {
            FullAuditHandler fullAuditHandler = new FullAuditHandler(handler);
            handler = fullAuditHandler;
            ModuleRegistry.registerModule(FullAuditHandler.class.getName(), FullAuditHandler.config, null);
        }

        // check if body handler is enabled
        BodyConfig bodyConfig = (BodyConfig)Config.getInstance().getJsonObjectConfig(BodyHandler.CONFIG_NAME, BodyConfig.class);
        if(bodyConfig.isEnableBodyHandler()) {
            BodyHandler bodyHandler = new BodyHandler(handler);
            handler = bodyHandler;
            ModuleRegistry.registerModule(BodyHandler.class.getName(),
                    Config.getInstance().getJsonMapConfigNoCache(BodyHandler.CONFIG_NAME), null);
        }

        // check if jwt token verification is enabled
        object = Config.getInstance().getJsonMapConfig(JwtHelper.SECURITY_CONFIG).get(JwtHelper.ENABLE_VERIFY_JWT);
        if(object != null && (Boolean)object == true) {
            JwtVerifyHandler jwtVerifyHandler = new JwtVerifyHandler(handler);
            handler = jwtVerifyHandler;
            ModuleRegistry.registerModule(JwtVerifyHandler.class.getName(),
                    Config.getInstance().getJsonMapConfigNoCache(JwtHelper.SECURITY_CONFIG), null);
        }

        // enable swagger handler by default, this requires swagger.json in the config folder always.
        SwaggerHandler swaggerHandler = new SwaggerHandler(handler);
        handler = swaggerHandler;
        ModuleRegistry.registerModule(SwaggerHandler.class.getName(),
                Config.getInstance().getJsonMapConfigNoCache(SwaggerHandler.CONFIG_NAME), null);

        // check if exception handler is enabled
        ExceptionConfig exceptionConfig = (ExceptionConfig)Config.getInstance().getJsonObjectConfig(ExceptionHandler.CONFIG_NAME, ExceptionConfig.class);
        if(exceptionConfig.isEnableExceptionHandler()) {
            ExceptionHandler exceptionHandler = new ExceptionHandler(handler);
            handler = exceptionHandler;
            ModuleRegistry.registerModule(ExceptionHandler.class.getName(),
                    Config.getInstance().getJsonMapConfigNoCache(ExceptionHandler.CONFIG_NAME), null);
        }
        */

        server = Undertow.builder()
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
                .build();
        server.start();
    }

    static public void stop() {
        if (server != null) server.stop();
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

}
