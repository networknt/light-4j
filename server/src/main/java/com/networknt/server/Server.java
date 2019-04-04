/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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

import com.networknt.common.SecretConstants;
import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.HandlerProvider;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.handler.OrchestrationHandler;
import com.networknt.registry.Registry;
import com.networknt.registry.URL;
import com.networknt.registry.URLImpl;
import com.networknt.service.SingletonServiceFactory;
import com.networknt.switcher.SwitcherUtil;
import com.networknt.utility.Constants;
import com.networknt.utility.Util;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.GracefulShutdownHandler;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.xnio.Options;
import org.xnio.SslClientAuthMode;

import javax.net.ssl.*;
import java.io.InputStream;
import java.net.InetAddress;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.*;

/**
 * This is the entry point of the framework. It wrapped Undertow Core HTTP
 * server and controls the lifecycle of the server. It also orchestrate
 * different types of plugins and wire them in at the right location.
 *
 * @author Steve Hu
 */
public class Server {

    static final Logger logger = LoggerFactory.getLogger(Server.class);
    public static final String SERVER_CONFIG_NAME = "server";
    public static final String SECRET_CONFIG_NAME = "secret";
    public static final String[] STATUS_CONFIG_NAME = {"status", "app-status"};
    
    public static final String ENV_PROPERTY_KEY = "environment";

    static final String STATUS_HOST_IP = "STATUS_HOST_IP";

    // service_id in slf4j MDC
    static final String SID = "sId";

    public final static TrustManager[] TRUST_ALL_CERTS = new X509TrustManager[]{new DummyTrustManager()};

    static protected boolean shutdownRequested = false;
    static Undertow server = null;
    static URL serviceUrl;
    static Registry registry;

    static SSLContext sslContext;

    static GracefulShutdownHandler gracefulShutdownHandler;
    
    public static void main(final String[] args) {
        init();
    }

    public static void init() {
        logger.info("server starts");
        // setup system property to redirect undertow logs to slf4j/logback.
        System.setProperty("org.jboss.logging.provider", "slf4j");
        // this will make sure that all log statement will have serviceId
        MDC.put(SID, getServerConfig().getServiceId());

        try {
            start();

            // merge status.yml and app-status.yml if app-status.yml is provided
            mergeStatusConfig();
        } catch (RuntimeException e) {
            // Handle any exception encountered during server start-up
            logger.error("Server is not operational! Failed with exception", e);

            // send a graceful system shutdown
            System.exit(1);
        }
    }

    static public void start() {
        // add shutdown hook here.
        addDaemonShutdownHook();

        // add startup hooks here.
        StartupHookProvider[] startupHookProviders = SingletonServiceFactory.getBeans(StartupHookProvider.class);
        if (startupHookProviders != null)
            Arrays.stream(startupHookProviders).forEach(s -> s.onStartup());

        // For backwards compatibility, check if a handler.yml has been included. If
        // not, default to original configuration.
        if (Handler.config == null || !Handler.config.isEnabled()) {
            HttpHandler handler = middlewareInit();

            // register the graceful shutdown handler
            gracefulShutdownHandler = new GracefulShutdownHandler(handler);
        } else {
            // initialize the handlers, chains and paths
            Handler.init();

            // register the graceful shutdown handler
            gracefulShutdownHandler = new GracefulShutdownHandler(new OrchestrationHandler());
        }

       ServerConfig serverConfig = getServerConfig();

        if (serverConfig.dynamicPort) {
            if (serverConfig.minPort > serverConfig.maxPort) {
                String errMessage = "No ports available to bind to - the minPort is larger than the maxPort in server.yml";
                logger.error(errMessage);
                throw new RuntimeException(errMessage);
            }          
            for (int i = serverConfig.minPort; i < serverConfig.maxPort; i++) {
                boolean b = bind(gracefulShutdownHandler, i);
                if (b) {
                    break;
                }
            }
        } else {
            bind(gracefulShutdownHandler, -1);
        }
    }

    private static HttpHandler middlewareInit() {
        HttpHandler handler = null;

        // API routing handler or others handler implemented by application developer.
        HandlerProvider handlerProvider = SingletonServiceFactory.getBean(HandlerProvider.class);
        if (handlerProvider != null) {
            handler = handlerProvider.getHandler();
        }
        if (handler == null) {
            logger.error("Unable to start the server - no route handler provider available in service.yml");
            throw new RuntimeException(
                    "Unable to start the server - no route handler provider available in service.yml");
        }
        // Middleware Handlers plugged into the handler chain.
        MiddlewareHandler[] middlewareHandlers = SingletonServiceFactory.getBeans(MiddlewareHandler.class);
        if (middlewareHandlers != null) {
            for (int i = middlewareHandlers.length - 1; i >= 0; i--) {
                logger.info("Plugin: " + middlewareHandlers[i].getClass().getName());
                if (middlewareHandlers[i].isEnabled()) {
                    handler = middlewareHandlers[i].setNext(handler);
                    middlewareHandlers[i].register();
                }
            }
        }
        return handler;
    }

    static private boolean bind(HttpHandler handler, int port) {
        ServerConfig serverConfig = getServerConfig();

        try {
            Undertow.Builder builder = Undertow.builder();
            if (serverConfig.enableHttps) {
                port = port < 0 ? serverConfig.getHttpsPort() : port;
                sslContext = createSSLContext();
                builder.addHttpsListener(port, serverConfig.getIp(), sslContext);
            } else if (serverConfig.enableHttp) {
                port = port < 0 ? serverConfig.getHttpPort() : port;
                builder.addHttpListener(port, serverConfig.getIp());
            } else {
                throw new RuntimeException(
                        "Unable to start the server as both http and https are disabled in server.yml");
            }

            if (serverConfig.enableHttp2) {
                builder.setServerOption(UndertowOptions.ENABLE_HTTP2, true);
            }

            if (serverConfig.isEnableTwoWayTls()) {
               builder.setSocketOption(Options.SSL_CLIENT_AUTH_MODE, SslClientAuthMode.REQUIRED);
            }

            server = builder.setBufferSize(1024 * 16).setIoThreads(Runtime.getRuntime().availableProcessors() * 2)
                    // above seems slightly faster in some configurations
                    .setSocketOption(Options.BACKLOG, 10000)
                    .setServerOption(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, false) // don't send a keep-alive header for
                    // HTTP/1.1 requests, as it is not required
                    .setServerOption(UndertowOptions.ALWAYS_SET_DATE, true)
                    .setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, false)
                    .setHandler(Handlers.header(handler, Headers.SERVER_STRING, "L")).setWorkerThreads(200).build();

            server.start();
            System.out.println("HOST IP " + System.getenv(STATUS_HOST_IP));
        } catch (Exception e) {
            if (!serverConfig.dynamicPort || (serverConfig.dynamicPort && serverConfig.maxPort == port + 1)) {
                String triedPortsMessage = serverConfig.dynamicPort ? serverConfig.minPort + " to: " + (serverConfig.maxPort - 1) : port + "";
                String errMessage = "No ports available to bind to. Tried: " + triedPortsMessage;
                System.out.println(errMessage);
                logger.error(errMessage);
                throw new RuntimeException(errMessage, e);
            }
            System.out.println("Failed to bind to port " + port + ". Trying " + ++port);
            if (logger.isInfoEnabled())
                logger.info("Failed to bind to port " + port + ". Trying " + ++port);
            return false;
        }
        // application level service registry. only be used without docker container.
        if (serverConfig.enableRegistry) {
            // assuming that registry is defined in service.json, otherwise won't start
            // server.
            try {
                registry = SingletonServiceFactory.getBean(Registry.class);
                if (registry == null)
                    throw new RuntimeException("Could not find registry instance in service map");
                // in kubernetes pod, the hostIP is passed in as STATUS_HOST_IP environment
                // variable. If this is null
                // then get the current server IP as it is not running in Kubernetes.
                String ipAddress = System.getenv(STATUS_HOST_IP);
                logger.info("Registry IP from STATUS_HOST_IP is " + ipAddress);
                if (ipAddress == null) {
                    InetAddress inetAddress = Util.getInetAddress();
                    ipAddress = inetAddress.getHostAddress();
                    logger.info("Could not find IP from STATUS_HOST_IP, use the InetAddress " + ipAddress);
                }
                Map parameters = new HashMap<>();
                if (serverConfig.getEnvironment() != null)
                    parameters.put(ENV_PROPERTY_KEY, serverConfig.getEnvironment());
                serviceUrl = new URLImpl("light", ipAddress, port, getServerConfig().getServiceId(), parameters);
                registry.register(serviceUrl);
                if (logger.isInfoEnabled())
                    logger.info("register service: " + serviceUrl.toFullStr());

                // start heart beat if registry is enabled
                SwitcherUtil.setSwitcherValue(Constants.REGISTRY_HEARTBEAT_SWITCHER, true);
                if (logger.isInfoEnabled())
                    logger.info("Registry heart beat switcher is on");
                // handle the registration exception separately to eliminate confusion
            } catch (Exception e) {
                System.out.println("Failed to register service, the server stopped.");
                if (logger.isInfoEnabled())
                    logger.info("Failed to register service, the server stopped.");
                throw new RuntimeException(e.getMessage());
            }
        }

        if (serverConfig.enableHttp) {
            System.out.println("Http Server started on ip:" + serverConfig.getIp() + " Port:" + port);
            if (logger.isInfoEnabled())
                logger.info("Http Server started on ip:" + serverConfig.getIp() + " Port:" + port);
        } else {
            System.out.println("Http port disabled.");
            if (logger.isInfoEnabled())
                logger.info("Http port disabled.");
        }
        if (serverConfig.enableHttps) {
            System.out.println("Https Server started on ip:" + serverConfig.getIp() + " Port:" + port);
            if (logger.isInfoEnabled())
                logger.info("Https Server started on ip:" + serverConfig.getIp() + " Port:" + port);
        } else {
            System.out.println("Https port disabled.");
            if (logger.isInfoEnabled())
                logger.info("Https port disabled.");
        }

        return true;
    }

    static public void stop() {
        if (server != null)
            server.stop();
    }

    // implement shutdown hook here.
    static public void shutdown() {
        ServerConfig serverConfig = getServerConfig();

        // need to unregister the service
        if (serverConfig.enableRegistry && registry != null) {
            registry.unregister(serviceUrl);
            // Please don't remove the following line. When server is killed, the logback
            // won't work anymore.
            // Even debugger won't reach this point; however, the logic is executed
            // successfully here.
            System.out.println("unregister serviceUrl " + serviceUrl);
            if (logger.isInfoEnabled())
                logger.info("unregister serviceUrl " + serviceUrl);
        }

        if (gracefulShutdownHandler != null) {
            logger.info("Starting graceful shutdown.");
            gracefulShutdownHandler.shutdown();
            try {
                gracefulShutdownHandler.awaitShutdown(60 * 1000);
            } catch (InterruptedException e) {
                logger.error("Error occurred while waiting for pending requests to complete.", e);
            }
            logger.info("Graceful shutdown complete.");
        }

        ShutdownHookProvider[] shutdownHookProviders = SingletonServiceFactory.getBeans(ShutdownHookProvider.class);
        if (shutdownHookProviders != null)
            Arrays.stream(shutdownHookProviders).forEach(s -> s.onShutdown());

        stop();
        logger.info("Cleaning up before server shutdown");
    }

    static protected void addDaemonShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                Server.shutdown();
            }
        });
    }

    private static KeyStore loadKeyStore() {
        ServerConfig serverConfig = getServerConfig();
        Map<String, Object> secretConfig = Config.getInstance().getJsonMapConfig(SECRET_CONFIG_NAME);

        String name = serverConfig.getKeystoreName();
        try (InputStream stream = Config.getInstance().getInputStreamFromFile(name)) {
            KeyStore loadedKeystore = KeyStore.getInstance("JKS");
            loadedKeystore.load(stream, ((String) secretConfig.get(SecretConstants.SERVER_KEYSTORE_PASS)).toCharArray());
            return loadedKeystore;
        } catch (Exception e) {
            logger.error("Unable to load keystore " + name, e);
            throw new RuntimeException("Unable to load keystore " + name, e);
        }
    }

    protected static KeyStore loadTrustStore() {
        ServerConfig serverConfig = getServerConfig();
        Map<String, Object> secretConfig = Config.getInstance().getJsonMapConfig(SECRET_CONFIG_NAME);

        String name = serverConfig.getTruststoreName();
        try (InputStream stream = Config.getInstance().getInputStreamFromFile(name)) {
            KeyStore loadedKeystore = KeyStore.getInstance("JKS");
            loadedKeystore.load(stream, ((String) secretConfig.get(SecretConstants.SERVER_TRUSTSTORE_PASS)).toCharArray());
            return loadedKeystore;
        } catch (Exception e) {
            logger.error("Unable to load truststore " + name, e);
            throw new RuntimeException("Unable to load truststore " + name, e);
        }
    }

    private static TrustManager[] buildTrustManagers(final KeyStore trustStore) {
        TrustManager[] trustManagers = null;
        if (trustStore != null) {
            try {
                TrustManagerFactory trustManagerFactory = TrustManagerFactory
                        .getInstance(KeyManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(trustStore);
                trustManagers = trustManagerFactory.getTrustManagers();
            } catch (NoSuchAlgorithmException | KeyStoreException e) {
                logger.error("Unable to initialise TrustManager[]", e);
                throw new RuntimeException("Unable to initialise TrustManager[]", e);
            }
        } else {
            logger.warn("Unable to find server truststore while Mutual TLS is enabled. Falling back to trust all certs.");
            trustManagers = TRUST_ALL_CERTS;
        }
        return trustManagers;
    }

    private static KeyManager[] buildKeyManagers(final KeyStore keyStore, char[] keyPass) {
        KeyManager[] keyManagers;
        try {
            KeyManagerFactory keyManagerFactory = KeyManagerFactory
                    .getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyPass);
            keyManagers = keyManagerFactory.getKeyManagers();
        } catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException e) {
            logger.error("Unable to initialise KeyManager[]", e);
            throw new RuntimeException("Unable to initialise KeyManager[]", e);
        }
        return keyManagers;
    }

    private static SSLContext createSSLContext() throws RuntimeException {
        ServerConfig serverConfig = getServerConfig();
        Map<String, Object> secretConfig = Config.getInstance().getJsonMapConfig(SECRET_CONFIG_NAME);

        try {
            KeyManager[] keyManagers = buildKeyManagers(loadKeyStore(),
                    ((String) secretConfig.get(SecretConstants.SERVER_KEY_PASS)).toCharArray());
            TrustManager[] trustManagers;
            if (serverConfig.isEnableTwoWayTls()) {
                trustManagers = buildTrustManagers(loadTrustStore());
            } else {
                trustManagers = buildTrustManagers(null);
            }

            SSLContext sslContext;
            sslContext = SSLContext.getInstance("TLSv1");
            sslContext.init(keyManagers, trustManagers, null);
            return sslContext;
        } catch (Exception e) {
            logger.error("Unable to create SSLContext", e);
            throw new RuntimeException("Unable to create SSLContext", e);
        }
    }


    // method used to merge status.yml and app-status.yml
    protected static void mergeStatusConfig() {
        Map<String, Object> appStatusConfig = Config.getInstance().getJsonMapConfigNoCache(STATUS_CONFIG_NAME[1]);
        if (appStatusConfig == null) {
            return;
        }
        Map<String, Object> statusConfig = Config.getInstance().getJsonMapConfig(STATUS_CONFIG_NAME[0]);
        // clone the default status config key set
        Set<String> duplicatedStatusSet = new HashSet<>(statusConfig.keySet());
        duplicatedStatusSet.retainAll(appStatusConfig.keySet());
        if (!duplicatedStatusSet.isEmpty()) {
            logger.error("The status code(s): " + duplicatedStatusSet.toString() + " is already in use by light-4j and cannot be overwritten," +
                    " please change to another status code in app-status.yml if necessary.");
            throw new RuntimeException("The status code(s): " + duplicatedStatusSet.toString() + " in status.yml and app-status.yml are duplicated.");
        }
        statusConfig.putAll(appStatusConfig);
    }

    public static ServerConfig getServerConfig(){
        return (ServerConfig) Config.getInstance().getJsonObjectConfig(SERVER_CONFIG_NAME,
                ServerConfig.class);
    }
}
