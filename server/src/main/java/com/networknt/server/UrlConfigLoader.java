package com.networknt.server;

import static com.networknt.server.Server.ENV_PROPERTY_KEY;
import static com.networknt.server.Server.STARTUP_CONFIG_NAME;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.jose4j.json.internal.json_simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.utility.StringUtils;

import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.Headers;
import io.undertow.util.Methods;

/**
 * UrlConfigLoader fetch and load configs from Nginx. The config files share the
 * same structure as FileSystemProviderImpl.
 * 
 * @author xlongwei
 *
 */
public class UrlConfigLoader implements IConfigLoader {
	static final Logger logger = LoggerFactory.getLogger(UrlConfigLoader.class);

	public static final String CONFIG_SERVER_URI = "light-config-server-uri";
	public static final String CONFIG_SERVER_PATH = "light-config-server-path";
	public static String configServerUri = System.getProperty(CONFIG_SERVER_URI);
	public static String configServerPath = System.getProperty(CONFIG_SERVER_PATH, "/light-service-configs");

	public static final String LIGHT_ENV = "light-env";
	public static final String DEFAULT_ENV = "dev";
	public static final String DEFAULT_TARGET_CONFIGS_DIRECTORY = "src/main/resources/config";
	public static String lightEnv = System.getProperty(LIGHT_ENV, DEFAULT_ENV);
	public static String targetConfigsDirectory = System.getProperty(Config.LIGHT_4J_CONFIG_DIR,
			DEFAULT_TARGET_CONFIGS_DIRECTORY);

	public static Map<String, Object> startupConfig = Config.getInstance().getJsonMapConfig(STARTUP_CONFIG_NAME);
	public static final String PROJECT_NAME = "projectName";
	public static final String PROJECT_VERSION = "projectVersion";
	public static final String SERVICE_NAME = "serviceName";
	public static final String SERVICE_VERSION = "serviceVersion";

	public static final String AUTHORIZATION = "config_server_authorization";
	public static final String CLIENT_TRUSTSTORE_PASS = "config_server_client_truststore_password";
	public static final String CLIENT_TRUSTSTORE_LOC = "config_server_client_truststore_location";
	public static final String VERIFY_HOST_NAME = "config_server_client_verify_host_name";
	public static final String clientToken = System.getenv(AUTHORIZATION);

	public static final String CONFIGS = "configs";
	public static final String FILES = "files";
	public static final String CERTS = "certs";
	public static final String GLOBALS = "globals";
	public static final String SLASH = "/";

	static final String CONFIGS_FILE_NAME = "values";
	static final String CONFIG_EXT_JSON = ".json";
	static final String CONFIG_EXT_YAML = ".yaml";
	static final String CONFIG_EXT_YML = ".yml";
	static final String[] configExtensionsOrdered = { CONFIG_EXT_YML, CONFIG_EXT_YAML, CONFIG_EXT_JSON };

	final static Yaml yaml = new Yaml();
	final static ObjectMapper mapper = new ObjectMapper();
	final static TypeReference<Map<String, Object>> mapType = new TypeReference<Map<String, Object>>() {
	};
	final static Pattern files = Pattern.compile(">([^>/]+)</a>");
	static Http2Client client = Http2Client.getInstance();
	ClientConnection connection = null;
	String host = null;

	@Override
	public void init() {
		if (StringUtils.isBlank(configServerUri)) {
			return;
		}
		try {
			logger.info("init url config: {}{}", configServerUri, configServerPath);
			URI uri = new URI(configServerUri);
			host = uri.getHost();
			connection = client.connect(uri, Http2Client.WORKER, client.createXnioSsl(createBootstrapContext()),
					Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();

			loadConfigs();

			loadFiles(CERTS);

			loadFiles(FILES);
		} catch (Exception e) {
			logger.error("Failed to connect to config server", e);
		} finally {
			// here the connection is closed after one request. It should be used for in
			// frequent
			// request as creating a new connection is costly with TLS handshake and ALPN.
			IoUtils.safeClose(connection);
		}
	}

	@Override
	public void reloadConfig() {
		if (StringUtils.isBlank(configServerUri)) {
			return;
		}
		try {
			logger.info("init url config: {}{}", configServerUri, configServerPath);
			URI uri = new URI(configServerUri);
			host = uri.getHost();
			connection = client.connect(uri, Http2Client.WORKER, client.createXnioSsl(createBootstrapContext()),
					Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();

			loadConfigs();

		} catch (Exception e) {
			logger.error("Failed to connect to config server", e);
		} finally {
			// here the connection is closed after one request. It should be used for in
			// frequent
			// request as creating a new connection is costly with TLS handshake and ALPN.
			IoUtils.safeClose(connection);
		}
	}

	@SuppressWarnings("deprecation")
	private void loadConfigs() {
		Map<String, Object> serviceConfigs = new HashMap<>();
		serviceConfigs.putAll(getServiceConfigs(true));
		serviceConfigs.putAll(getServiceConfigs(false));
		serviceConfigs.put(ENV_PROPERTY_KEY, lightEnv);
		logger.debug("loadConfigs: {}", serviceConfigs);

		// pass serviceConfigs through Config.yaml's load method so that it can decrypt
		// any encrypted values
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);// to get yaml string without curly brackets and
																	// commas
		serviceConfigs = Config.getInstance().getYaml().load(new Yaml(options).dump(serviceConfigs));

		// clear config cache: this is required just in case other classes have already
		// loaded something in cache
		Config.getInstance().clear();
		Config.getInstance().putInConfigCache(CONFIGS_FILE_NAME, serviceConfigs);
		// You can call Server.getServerConfig() from now.
	}

	private void loadFiles(String configType) {
		Map<String, byte[]> serviceFiles = new HashMap<>();
		serviceFiles.putAll(getServiceFiles(configType, true));
		serviceFiles.putAll(getServiceFiles(configType, false));
		logger.debug("{} files loaded from config sever.", serviceFiles.size());
		logger.debug("loadFiles: {}", serviceFiles);
		try {
			Path filePath = Paths.get(targetConfigsDirectory);
			if (!Files.exists(filePath)) {
				Files.createDirectories(filePath);
				logger.info("target configs directory created :", targetConfigsDirectory);
			}
			for (String fileName : serviceFiles.keySet()) {
				filePath = Paths.get(targetConfigsDirectory + "/" + fileName);
				Files.write(filePath, serviceFiles.get(fileName));
			}
		} catch (IOException e) {
			logger.error("Exception while creating {} dir or creating files there:{}", targetConfigsDirectory, e);
		}
	}

	private Map<String, Object> getServiceConfigs(boolean globals) {
		Map<String, Object> config;
		for (String extension : configExtensionsOrdered) {
			config = loadSpecificConfigFileAsMap(CONFIGS, globals, extension);
			if (config != null) {
				return config;
			}
		}
		return Collections.emptyMap();
	}

	private Map<String, byte[]> getServiceFiles(String configType, boolean globals) {
		Map<String, byte[]> serviceFiles = new HashMap<>();
		String configPath = getConfigServerPath(configType, globals) + SLASH;
		String respBody = getString(configPath);
		if (StringUtils.isNotBlank(respBody)) {
			Matcher matcher = files.matcher(respBody);
			String endpoint = null;
			while (matcher.find()) {
				String file = matcher.group(1);
				endpoint = configPath + SLASH + file;
				byte[] bs = getBytes(endpoint);
				if (bs != null && bs.length > 0) {
					serviceFiles.put(file, bs);
				}
			}
		}
		return serviceFiles;
	}

	private Map<String, Object> loadSpecificConfigFileAsMap(String configType, boolean globals, String fileExtension) {
		String endpoint = getConfigServerPath(configType, globals) + SLASH + CONFIGS_FILE_NAME + fileExtension;
		String respBody = getString(endpoint);
		if (StringUtils.isNotBlank(respBody)) {
			try {
				if (!CONFIG_EXT_JSON.equals(fileExtension)) {
					respBody = JSONValue.toJSONString(yaml.load(respBody));
				}
				return mapper.readValue(respBody, mapType);
			} catch (Exception e) {
				logger.error("Exception while parsing Url response: {} {}", e.getClass().getSimpleName(),
						e.getMessage());
			}
		}
		return null;
	}

	private static String getConfigServerPath(String configType, boolean globals) {
		StringBuilder configPath = new StringBuilder(configServerPath);
		configPath.append(SLASH).append(configType);
		configPath.append(SLASH).append(startupConfig.get(PROJECT_NAME));
		if (globals) {
			configPath.append(SLASH).append(GLOBALS);
			configPath.append(SLASH).append(startupConfig.get(PROJECT_VERSION));
		} else {
			configPath.append(SLASH).append(startupConfig.get(SERVICE_NAME));
			configPath.append(SLASH).append(startupConfig.get(SERVICE_VERSION));
		}
		configPath.append(SLASH).append(lightEnv);
		return configPath.toString();
	}

	private String getString(String endpoint) {
		Object object = sendRequest(clientToken, endpoint, false);
		return object == null ? null : (String) object;
	}

	private byte[] getBytes(String endpoint) {
		Object object = sendRequest(clientToken, endpoint, true);
		if (object != null) {
			ByteBuffer buffer = (ByteBuffer) object;
			return buffer.array();
		}
		return null;
	}

	private Object sendRequest(String clientToken, String endpoint, boolean raw) {
		try {
			logger.debug("GET url configs endpoint:{}{}", configServerUri, endpoint);
			ClientRequest clientRequest = new ClientRequest().setMethod(Methods.GET).setPath(endpoint);
			if (StringUtils.isNotBlank(clientToken)) {
				clientRequest.getRequestHeaders().put(Headers.AUTHORIZATION, clientToken);
			}
			clientRequest.getRequestHeaders().put(Headers.HOST, host);

			// Send the request
			AtomicReference<ClientResponse> responseReference = new AtomicReference<>();
			CountDownLatch latch = new CountDownLatch(1);
			connection.sendRequest(clientRequest, raw ? client.byteBufferClientCallback(responseReference, latch)
					: client.createClientCallback(responseReference, latch));

			latch.await(10000, TimeUnit.MILLISECONDS);

			ClientResponse clientResponse = responseReference.get();
			if (clientResponse != null) {
				int statusCode = clientResponse.getResponseCode();

				if (statusCode == 200) {
					return raw ? clientResponse.getAttachment(Http2Client.BUFFER_BODY)
							: clientResponse.getAttachment(Http2Client.RESPONSE_BODY);
				} else if (statusCode == 404) {
					return null;
				}
			}
			logger.debug("Received client response: {}", clientResponse);
		} catch (Exception e) {
			logger.error("Exception while GET url: {} {}", e.getClass().getSimpleName(), e.getMessage());
		}
		return null;
	}

	private static SSLContext createBootstrapContext() throws RuntimeException {
		SSLContext sslContext = null;
		try {
			TrustManager[] trustManagers = buildTrustManagers(loadBootstrapTrustStore());
			sslContext = SSLContext.getInstance("TLSv1.2");
			sslContext.init(null, trustManagers, null);
		} catch (Exception e) {
			logger.error("Unable to create SSLContext: {} {}", e.getClass().getSimpleName(), e.getMessage());
		}
		return sslContext;
	}

	private static TrustManager[] buildTrustManagers(final KeyStore trustStore) {
		try {
			if (trustStore != null) {
				TrustManagerFactory trustManagerFactory = TrustManagerFactory
						.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				trustManagerFactory.init(trustStore);
				return trustManagerFactory.getTrustManagers();
			}
		} catch (Exception e) {
			logger.error("Unable to initialise TrustManager[]", e);
		}
		return Server.TRUST_ALL_CERTS;
	}

	private static KeyStore loadBootstrapTrustStore() {
		String truststorePassword = System.getenv(CLIENT_TRUSTSTORE_PASS);
		String truststoreLocation = System.getenv(CLIENT_TRUSTSTORE_LOC);
		if (truststorePassword == null && truststorePassword == null) {
			truststorePassword = Server.getServerConfig().getBootstrapStorePass();
			truststorePassword = Server.getServerConfig().getBootstrapStorePass();
		}
		if (StringUtils.isBlank(truststoreLocation)) {
			return null;
		}

		try (InputStream stream = new FileInputStream(truststoreLocation)) {
			KeyStore loadedKeystore = KeyStore.getInstance("JKS");
			loadedKeystore.load(stream, truststorePassword != null ? truststorePassword.toCharArray() : null);
			return loadedKeystore;
		} catch (Exception e) {
			logger.error("Unable to load truststore: " + truststoreLocation, e);
			return null;
		}
	}
}
