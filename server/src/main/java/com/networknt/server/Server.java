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
import com.networknt.common.DecryptUtil;
import com.networknt.common.SecretConstants;
import com.networknt.config.Config;
import com.networknt.handler.HandlerProvider;
import com.networknt.handler.Handler;
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
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.GracefulShutdownHandler;
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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This is the entry point of the framework. It wrapped Undertow Core HTTP
 * server and controls the lifecycle of the server. It also orchestrate
 * different types of plugins and wire them in at the right location.
 *
 * @author Steve Hu
 */
public class Server {

	static final Logger logger = LoggerFactory.getLogger(Server.class);
	public static final String CONFIG_NAME = "server";
	public static final String CONFIG_SECRET = "secret";

	static final String DEFAULT_ENV = "test";
	static final String LIGHT_ENV = "light-env";
	static final String LIGHT_CONFIG_SERVER_URI = "light-config-server-uri";
	static final String STATUS_HOST_IP = "STATUS_HOST_IP";

	// service_id in slf4j MDC
	static final String SID = "sId";

	public static ServerConfig config = (ServerConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME,
			ServerConfig.class);
	public static Map<String, Object> secret = DecryptUtil
			.decryptMap(Config.getInstance().getJsonMapConfig(CONFIG_SECRET));
	public final static TrustManager[] TRUST_ALL_CERTS = new X509TrustManager[] { new DummyTrustManager() };

	static protected boolean shutdownRequested = false;
	static Undertow server = null;
	static URL serviceUrl;
	static Registry registry;

	static SSLContext sslContext;

	static GracefulShutdownHandler gracefulShutdownHandler;

	public static void main(final String[] args) {
		logger.info("server starts");
		// setup system property to redirect undertow logs to slf4j/logback.
		System.setProperty("org.jboss.logging.provider", "slf4j");
		// this will make sure that all log statement will have serviceId
		MDC.put(SID, config.getServiceId());

		try {
			// load config files from light-config-server if possible.
			loadConfig();
			start();
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

		if (config.dynamicPort) {
			for (int i = config.minPort; i < config.maxPort; i++) {
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
		try {
			Undertow.Builder builder = Undertow.builder();
			if (config.enableHttps) {
				port = port < 0 ? config.getHttpsPort() : port;
				sslContext = createSSLContext();
				builder.addHttpsListener(port, config.getIp(), sslContext);
			} else if (config.enableHttp) {
				port = port < 0 ? config.getHttpPort() : port;
				builder.addHttpListener(port, config.getIp());
			} else {
				throw new RuntimeException(
						"Unable to start the server as both http and https are disabled in server.yml");
			}

			if (config.enableHttp2) {
				builder.setServerOption(UndertowOptions.ENABLE_HTTP2, true);
			}

			server = builder.setBufferSize(1024 * 16).setIoThreads(Runtime.getRuntime().availableProcessors() * 2) // this
																													// seems
																													// slightly
																													// faster
																													// in
																													// some
																													// configurations
					.setSocketOption(Options.BACKLOG, 10000)
					.setServerOption(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, false) // don't send a keep-alive header for
																					// HTTP/1.1 requests, as it is not
																					// required
					.setServerOption(UndertowOptions.ALWAYS_SET_DATE, true)
					.setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, false)
					.setHandler(Handlers.header(handler, Headers.SERVER_STRING, "L")).setWorkerThreads(200).build();

			server.start();
			System.out.println("HOST IP " + System.getenv(STATUS_HOST_IP));
			// application level service registry. only be used without docker container.
			if (config.enableRegistry) {
				// assuming that registry is defined in service.json, otherwise won't start
				// server.
				registry = SingletonServiceFactory.getBean(Registry.class);
				if (registry == null)
					throw new RuntimeException("Could not find registry instance in service map");
				// in kubernetes pod, the hostIP is passed in as STATUS_HOST_IP environment
				// variable. If this is null
				// then get the current server IP as it is not running in Kubernetes.
				String ipAddress = System.getenv(STATUS_HOST_IP);
				if (ipAddress == null) {
					InetAddress inetAddress = Util.getInetAddress();
					ipAddress = inetAddress.getHostAddress();
				}
				Map parameters = new HashMap<>();
				if (config.getEnvironment() != null)
					parameters.put("environment", config.getEnvironment());
				serviceUrl = new URLImpl("light", ipAddress, port, config.getServiceId(), parameters);
				registry.register(serviceUrl);
				if (logger.isInfoEnabled())
					logger.info("register service: " + serviceUrl.toFullStr());

				// start heart beat if registry is enabled
				SwitcherUtil.setSwitcherValue(Constants.REGISTRY_HEARTBEAT_SWITCHER, true);
				if (logger.isInfoEnabled())
					logger.info("Registry heart beat switcher is on");

			}

			if (config.enableHttp) {
				System.out.println("Http Server started on ip:" + config.getIp() + " Port:" + port);
				if (logger.isInfoEnabled())
					logger.info("Http Server started on ip:" + config.getIp() + " Port:" + port);
			} else {
				System.out.println("Http port disabled.");
				if (logger.isInfoEnabled())
					logger.info("Http port disabled.");
			}
			if (config.enableHttps) {
				System.out.println("Https Server started on ip:" + config.getIp() + " Port:" + port);
				if (logger.isInfoEnabled())
					logger.info("Https Server started on ip:" + config.getIp() + " Port:" + port);
			} else {
				System.out.println("Https port disabled.");
				if (logger.isInfoEnabled())
					logger.info("Https port disabled.");
			}

			return true;
		} catch (Exception e) {
			System.out.println("Failed to bind to port " + port);
			if (logger.isInfoEnabled())
				logger.info("Failed to bind to port " + port);
			return false;
		}
	}

	static public void stop() {
		if (server != null)
			server.stop();
	}

	// implement shutdown hook here.
	static public void shutdown() {

		// need to unregister the service
		if (config.enableRegistry && registry != null) {
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
		String name = config.getKeystoreName();
		try (InputStream stream = Config.getInstance().getInputStreamFromFile(name)) {
			KeyStore loadedKeystore = KeyStore.getInstance("JKS");
			loadedKeystore.load(stream, ((String) secret.get(SecretConstants.SERVER_KEYSTORE_PASS)).toCharArray());
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
			loadedKeystore.load(stream, ((String) secret.get(SecretConstants.SERVER_TRUSTSTORE_PASS)).toCharArray());
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
			} catch (NoSuchAlgorithmException | KeyStoreException e) {
				logger.error("Unable to initialise TrustManager[]", e);
				throw new RuntimeException("Unable to initialise TrustManager[]", e);
			}
		} else {
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
		try {
			KeyManager[] keyManagers = buildKeyManagers(loadKeyStore(),
					((String) secret.get(SecretConstants.SERVER_KEY_PASS)).toCharArray());
			TrustManager[] trustManagers;
			if (config.isEnableTwoWayTls()) {
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
	 * is dockerized and orchestrated by Kubernetes, the config files and secret
	 * will be mapped to Kubernetes ConfigMap and Secret and passed into the
	 * container.
	 *
	 * Of course, you can still use it with standalone docker container but it is
	 * not recommended.
	 */
	private static void loadConfig() {
		// if it is necessary to load config files from config server
		// Here we expect at least env(dev/sit/uat/prod) and optional config server url
		String env = System.getProperty(LIGHT_ENV);
		if (env == null) {
			logger.warn("Warning! No light-env has been passed in from command line. Default to dev");
			env = DEFAULT_ENV;
		}
		String configUri = System.getProperty(LIGHT_CONFIG_SERVER_URI);
		if (configUri != null) {
			// try to get config files from the server.
			String targetMergeDirectory = System.getProperty(Config.LIGHT_4J_CONFIG_DIR);
			if (targetMergeDirectory == null) {
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
				connection = client.connect(new URI(configUri), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL,
						OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
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

				if (statusCode >= 300) {
					logger.error("Failed to load config from config server" + statusCode + ":"
							+ reference.get().getAttachment(Http2Client.RESPONSE_BODY));
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
		// Open the file
		try (ZipFile file = new ZipFile(path)) {
			FileSystem fileSystem = FileSystems.getDefault();
			// Get file entries
			Enumeration<? extends ZipEntry> entries = file.entries();

			// We will unzip files in this folder
			Files.createDirectory(fileSystem.getPath(target));

			// Iterate over entries
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				// If directory then create a new directory in uncompressed folder
				if (entry.isDirectory()) {
					System.out.println("Creating Directory:" + target + entry.getName());
					Files.createDirectories(fileSystem.getPath(target + entry.getName()));
				}
				// Else create the file
				else {
					InputStream is = file.getInputStream(entry);
					BufferedInputStream bis = new BufferedInputStream(is);
					String uncompressedFileName = target + entry.getName();
					Path uncompressedFilePath = fileSystem.getPath(uncompressedFileName);
					Files.createFile(uncompressedFilePath);
					FileOutputStream fileOutput = new FileOutputStream(uncompressedFileName);
					while (bis.available() > 0) {
						fileOutput.write(bis.read());
					}
					fileOutput.close();
					System.out.println("Written :" + entry.getName());
				}
			}
		} catch (IOException e) {
			logger.error("IOException", e);
		}
	}
}
