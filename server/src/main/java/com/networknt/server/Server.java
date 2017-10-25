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

import com.networknt.client.Http2Client;
import com.networknt.common.SecretConfig;
import com.networknt.config.Config;
import com.networknt.handler.MiddlewareHandler;
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
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpHandler;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.Options;

import javax.net.ssl.*;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This is the entry point of the framework. It wrapped Undertow Core HTTP server
 * and controls the lifecycle of the server. It also orchestrate different types
 * of plugins and wire them in at the right location.
 *
 * @author Steve Hu
 */
public class Server {

    static final Logger logger = LoggerFactory.getLogger(Server.class);
    public static final String CONFIG_NAME = "server";
    public static final String CONFIG_SECRET = "secret";

    static final String DEFAULT_ENV = "dev";
    static final String LIGHT_ENV = "light-env";
    static final String LIGHT_CONFIG_SERVER_URI = "light-config-server-uri";

    // service_id in slf4j MDC
    static final String SID = "sId";

    public static ServerConfig config = (ServerConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ServerConfig.class);
    public static SecretConfig secret = (SecretConfig) Config.getInstance().getJsonObjectConfig(CONFIG_SECRET, SecretConfig.class);
    public final static TrustManager[] TRUST_ALL_CERTS = new X509TrustManager[] { new DummyTrustManager() };

    static protected boolean shutdownRequested = false;
    static Undertow server = null;
    static URL serviceHttpUrl;
    static URL serviceHttpsUrl;
    static Registry registry;

    static SSLContext sslContext;

    public static void main(final String[] args) {
        logger.info("server starts");
        // setup system property to redirect undertow logs to slf4j/logback.
        System.setProperty("org.jboss.logging.provider", "slf4j");
        // this will make sure that all log statement will have serviceId
        MDC.put(SID, config.getServiceId());
        // load config files from light-config-server if possible.
        loadConfig();
        start();
    }

    static public void start() {

        // add shutdown hook here.
        addDaemonShutdownHook();

        // add startup hooks here.
        final ServiceLoader<StartupHookProvider> startupLoaders = ServiceLoader.load(StartupHookProvider.class);
        for (final StartupHookProvider provider : startupLoaders) {
            provider.onStartup();
        }

        // application level service registry. only be used without docker container.
        if(config.enableRegistry) {
            // assuming that registry is defined in service.json, otherwise won't start server.
            registry = (Registry) SingletonServiceFactory.getBean(Registry.class);
            if(registry == null) throw new RuntimeException("Could not find registry instance in service map");
            InetAddress inetAddress = Util.getInetAddress();
            String ipAddress = inetAddress.getHostAddress();
            Map parameters = new HashMap<>();
            if(config.getEnvironment() != null) parameters.put("environment", config.getEnvironment());
            if(config.enableHttp) {
                serviceHttpUrl = new URLImpl("light", ipAddress, config.getHttpPort(), config.getServiceId(), parameters);
                registry.register(serviceHttpUrl);
                if(logger.isInfoEnabled()) logger.info("register service: " + serviceHttpUrl.toFullStr());
            }
            if(config.enableHttps) {
                serviceHttpsUrl = new URLImpl("light", ipAddress, config.getHttpsPort(), config.getServiceId(), parameters);
                registry.register(serviceHttpsUrl);
                if(logger.isInfoEnabled()) logger.info("register service: " + serviceHttpsUrl.toFullStr());
            }
        }

        HttpHandler handler = null;

        // API routing handler or others handler implemented by application developer.
        final ServiceLoader<HandlerProvider> handlerLoaders = ServiceLoader.load(HandlerProvider.class);
        for (final HandlerProvider provider : handlerLoaders) {
            if (provider.getHandler() != null) {
                handler = provider.getHandler();
                break;
            }
        }
        if (handler == null) {
            logger.error("Unable to start the server - no route handler provider available in the classpath");
            return;
        }

        // Middleware Handlers plugged into the handler chain.
        final ServiceLoader<MiddlewareHandler> middlewareLoaders = ServiceLoader.load(MiddlewareHandler.class);
        logger.debug("found middlewareLoaders", middlewareLoaders);
        for (final MiddlewareHandler middlewareHandler : middlewareLoaders) {
            logger.info("Plugin: " + middlewareHandler.getClass().getName());
            if(middlewareHandler.isEnabled()) {
                handler = middlewareHandler.setNext(handler);
                middlewareHandler.register();
            }
        }

        Undertow.Builder builder = Undertow.builder();

        if(config.enableHttp) {
            builder.addHttpListener(config.getHttpPort(), config.getIp());
        }
        if(config.enableHttps) {
            sslContext = createSSLContext();
            builder.addHttpsListener(config.getHttpsPort(), config.getIp(), sslContext);
        }
        if(config.enableHttp2) {
            builder.setServerOption(UndertowOptions.ENABLE_HTTP2, true);
        }

        server = builder
                .setBufferSize(1024 * 16)
                .setIoThreads(Runtime.getRuntime().availableProcessors() * 2) //this seems slightly faster in some configurations
                .setSocketOption(Options.BACKLOG, 10000)
                .setServerOption(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, false) //don't send a keep-alive header for HTTP/1.1 requests, as it is not required
                .setServerOption(UndertowOptions.ALWAYS_SET_DATE, true)
                .setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, false)
                .setHandler(Handlers.header(handler, Headers.SERVER_STRING, "L"))
                .setWorkerThreads(200)
                .build();
        server.start();

        if(logger.isInfoEnabled()) {
            if(config.enableHttp) {
                logger.info("Http Server started on ip:" + config.getIp() + " Port:" + config.getHttpPort());
            }
            if(config.enableHttps) {
                logger.info("Https Server started on ip:" + config.getIp() + " Port:" + config.getHttpsPort());
            }
        }

        if(config.enableRegistry) {
            // start heart beat if registry is enabled
            SwitcherUtil.setSwitcherValue(Constants.REGISTRY_HEARTBEAT_SWITCHER, true);
            if(logger.isInfoEnabled()) logger.info("Registry heart beat switcher is on");
        }
    }

    static public void stop() {
        if (server != null) server.stop();
    }

    // implement shutdown hook here.
    static public void shutdown() {

        // need to unregister the service
        if(config.enableRegistry && registry != null && config.enableHttp) {
            registry.unregister(serviceHttpUrl);
            if(logger.isInfoEnabled()) logger.info("unregister serviceHttpUrl " + serviceHttpUrl);
        }
        if(config.enableRegistry && registry != null && config.enableHttps) {
            registry.unregister(serviceHttpsUrl);
            if(logger.isInfoEnabled()) logger.info("unregister serviceHttpsUrl " + serviceHttpsUrl);
        }

        final ServiceLoader<ShutdownHookProvider> shutdownLoaders = ServiceLoader.load(ShutdownHookProvider.class);
        for (final ShutdownHookProvider provider : shutdownLoaders) {
            provider.onShutdown();
        }
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
        String name = config.getKeystoreName();
        try (InputStream stream = Config.getInstance().getInputStreamFromFile(name)) {
            KeyStore loadedKeystore = KeyStore.getInstance("JKS");
            loadedKeystore.load(stream, secret.getServerKeystorePass().toCharArray());
            return loadedKeystore;
        } catch (Exception e) {
            logger.error("Unable to load keystore " + name, e);
            throw new RuntimeException("Unable to load keystore " + name, e);
        }
    }

    protected static KeyStore loadTrustStore() {
        String name = config.getTruststoreName();
        try (InputStream stream = Config.getInstance().getInputStreamFromFile(name)) {
            KeyStore loadedKeystore = KeyStore.getInstance("JKS");
            loadedKeystore.load(stream, secret.getServerTruststorePass().toCharArray());
            return loadedKeystore;
        } catch (Exception e) {
            logger.error("Unable to load truststore " + name, e);
            throw new RuntimeException("Unable to load truststore " + name, e);
        }
    }

    private static TrustManager[] buildTrustManagers(final KeyStore trustStore) {
        TrustManager[] trustManagers = null;
        if (trustStore == null) {
            try {
                TrustManagerFactory trustManagerFactory = TrustManagerFactory
                        .getInstance(KeyManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(trustStore);
                trustManagers = trustManagerFactory.getTrustManagers();
            }
            catch (NoSuchAlgorithmException | KeyStoreException e) {
                logger.error("Unable to initialise TrustManager[]", e);
                throw new RuntimeException("Unable to initialise TrustManager[]", e);
            }
        }
        else {
            trustManagers = TRUST_ALL_CERTS;
        }
        return trustManagers;
    }

    private static KeyManager[] buildKeyManagers(final KeyStore keyStore, char[] keyPass) {
        KeyManager[] keyManagers;
        try {
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory
                    .getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyPass);
            keyManagers = keyManagerFactory.getKeyManagers();
        }
        catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException e) {
            logger.error("Unable to initialise KeyManager[]", e);
            throw new RuntimeException("Unable to initialise KeyManager[]", e);
        }
        return keyManagers;
    }

    private static SSLContext createSSLContext() throws RuntimeException {
        try {
            KeyManager[] keyManagers = buildKeyManagers(loadKeyStore(), secret.getServerKeyPass().toCharArray());
            TrustManager[] trustManagers;
            if(config.isEnableTwoWayTls()) {
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

    /**
     * Load config files from light-config-server instance. This is normally only
     * used when you run light-4j server as standalone java process. If the server
     * is dockerized and orchestrated by Kubernetes, the config files and secret will
     * be mapped to Kubernetes ConfigMap and Secret and passed into the container.
     *
     * Of course, you can still use it with standalone docker container but it is not
     * recommended.
     */
    private static void loadConfig() {
        // if it is necessary to load config files from config server
        // Here we expect at least env(dev/sit/uat/prod) and optional config server url
        String env = System.getProperty(LIGHT_ENV);
        if(env == null) {
            logger.warn("Warning! No light-env has been passed in from command line. Default to dev");
            env = DEFAULT_ENV;
        }
        String configUri = System.getProperty(LIGHT_CONFIG_SERVER_URI);
        if(configUri != null) {
            // try to get config files from the server.
            String targetMergeDirectory = System.getProperty(Config.LIGHT_4J_CONFIG_DIR);
            if(targetMergeDirectory == null) {
                logger.warn("Warning! No light-4j-config-dir has been passed in from command line.");
                return;
            }
            String version = Util.getJarVersion();
            String service = config.getServiceId();
            String tempDir = System.getProperty("java.io.tmpdir");
            String zipFile = tempDir + "/config.zip";
            // /v1/config/1.2.4/dev/com.networknt.petstore-1.0.0

            String path = "/v1/config/" + version + "/" + env + "/" + service;
            Http2Client client = Http2Client.getInstance();
            ClientConnection connection = null;
            try {
                connection = client.connect(new URI(configUri), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL, OptionMap.EMPTY).get();
            } catch (Exception e) {
                logger.error("Exeption:", e);
            }
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<ClientResponse> reference = new AtomicReference<>();
            try {
                ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(path);
                request.getRequestHeaders().put(Headers.HOST, "localhost");
                connection.sendRequest(request, client.createClientCallback(reference, latch));
                latch.await();
                int statusCode = reference.get().getResponseCode();

                if(statusCode >= 300){
                    logger.error("Failed to load config from config server" + statusCode + ":" + reference.get().getAttachment(Http2Client.RESPONSE_BODY));
                    throw new Exception("Failed to load config from config server: " + statusCode);
                } else {
                    // TODO test it out
                    FileOutputStream fos = new FileOutputStream(zipFile);
                    fos.write(reference.get().getAttachment(Http2Client.RESPONSE_BODY).getBytes());
                    fos.close();
                    unzipFile(zipFile, targetMergeDirectory);
                }
            } catch (Exception e) {
                logger.error("Exception:", e);
            } finally {
                IoUtils.safeClose(connection);
            }
        } else {
            logger.info("light-config-server-uri is missing in the command line. Use local config files");
        }
    }

    private static void mergeConfigFiles(String source, String target) {

    }
    private static void unzipFile(String path, String target) {
        //Open the file
        try(ZipFile file = new ZipFile(path))
        {
            FileSystem fileSystem = FileSystems.getDefault();
            //Get file entries
            Enumeration<? extends ZipEntry> entries = file.entries();

            //We will unzip files in this folder
            Files.createDirectory(fileSystem.getPath(target));

            //Iterate over entries
            while (entries.hasMoreElements())
            {
                ZipEntry entry = entries.nextElement();
                //If directory then create a new directory in uncompressed folder
                if (entry.isDirectory())
                {
                    System.out.println("Creating Directory:" + target + entry.getName());
                    Files.createDirectories(fileSystem.getPath(target + entry.getName()));
                }
                //Else create the file
                else
                {
                    InputStream is = file.getInputStream(entry);
                    BufferedInputStream bis = new BufferedInputStream(is);
                    String uncompressedFileName = target + entry.getName();
                    Path uncompressedFilePath = fileSystem.getPath(uncompressedFileName);
                    Files.createFile(uncompressedFilePath);
                    FileOutputStream fileOutput = new FileOutputStream(uncompressedFileName);
                    while (bis.available() > 0)
                    {
                        fileOutput.write(bis.read());
                    }
                    fileOutput.close();
                    System.out.println("Written :" + entry.getName());
                }
            }
        }
        catch(IOException e) {
            logger.error("IOException", e);
        }
    }
}
