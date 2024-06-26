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

import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Server configuration class that maps to server.yml properties. This class is a singleton and can be shared
 * by all modules.
 *
 * @author Steve Hu
 */
public class ServerConfig {
    public static Logger logger = LoggerFactory.getLogger(ServerConfig.class);

    public static final String CONFIG_NAME = "server";
    public static final String IP = "ip";
    public static final String HTTP_PORT = "httpPort";
    public static final String ENABLE_HTTP = "enableHttp";
    public static final String HTTPS_PORT = "httpsPort";
    public static final String ENABLE_HTTPS = "enableHttps";
    public static final String ENABLE_HTTP2 = "enableHttp2";
    public static final String KEYSTORE_NAME = "keystoreName";
    public static final String KEYSTORE_PASS = "keystorePass";
    public static final String KEY_PASS = "keyPass";
    public static final String ENABLE_TWO_WAY_TLS = "enableTwoWayTls";
    public static final String TRUSTSTORE_NAME = "truststoreName";
    public static final String TRUSTSTORE_PASS = "truststorePass";
    public static final String ENABLE_REGISTRY = "enableRegistry";
    public static final String SERVICE_ID = "serviceId";
    public static final String SERVICE_NAME = "serviceName";
    public static final String ENVIRONMENT = "environment";
    public static final String BUILD_NUMBER = "buildNumber";
    public static final String DYNAMIC_PORT = "dynamicPort";
    public static final String MIN_PORT = "minPort";
    public static final String MAX_PORT = "maxPort";
    public static final String BUFFER_SIZE = "bufferSize";
    public static final String IO_THREADS = "ioThreads";
    public static final String WORKER_THREADS = "workerThreads";
    public static final String BACKLOG = "backlog";
    public static final String SHUTDOWN_TIMEOUT = "shutdownTimeout";
    public static final String SHUTDOWN_GRACEFUL_PERIOD = "shutdownGracefulPeriod";
    public static final String ALWAYS_SET_DATE = "alwaysSetDate";
    public static final String ALLOW_UNESCAPED_CHARACTERS_IN_URL = "allowUnescapedCharactersInUrl";
    public static final String SERVER_STRING = "serverString";
    public static final String BOOTSTRAP_STORE_NAME = "bootstrapStoreName";
    public static final String BOOTSTRAP_STORE_PASS = "bootstrapStorePass";
    public static final String MAX_TRANSFER_FILE_SIZE = "maxTransferFileSize";
    public static final String START_ON_REGISTRY_FAILURE = "startOnRegistryFailure";
    public static final String MASK_CONFIG_PROPERTIES = "maskConfigProperties";

    String ip;
    int httpPort;
    boolean enableHttp;
    int httpsPort;
    boolean enableHttps;
    boolean enableHttp2;
    String keystoreName;
    String keystorePass;
    String keyPass;
    boolean enableTwoWayTls;
    String truststoreName;
    String truststorePass;
    boolean enableRegistry;
    String serviceId;
    String serviceName;
    String environment;
    String buildNumber;
    boolean dynamicPort;
    int minPort;
    int maxPort;
    int bufferSize;
    int ioThreads;
    int workerThreads;
    int backlog;
    int shutdownTimeout;
    int shutdownGracefulPeriod;
    boolean alwaysSetDate;
    boolean allowUnescapedCharactersInUrl;
    String serverString;
    String bootstrapStoreName;
    String bootstrapStorePass;
    long maxTransferFileSize;
    boolean startOnRegistryFailure;
    boolean maskConfigProperties;

    private final Config config;
    private final Map<String, Object> mappedConfig;
    private static volatile ServerConfig instance;

    private ServerConfig() {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfig(CONFIG_NAME);
        load();
    }

    /**
     * Please note that this constructor is only for testing to load different config files
     * to test different configurations.
     * @param configName String
     */
    private ServerConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        load();
    }


    private void load() {
        if(mappedConfig != null) {
            Object object = mappedConfig.get(IP);
            if(object != null) ip = (String)object;
            object = mappedConfig.get(HTTP_PORT);
            if(object != null) httpPort = Config.loadIntegerValue(HTTP_PORT, object);
            object = mappedConfig.get(ENABLE_HTTP);
            if(object != null) enableHttp = Config.loadBooleanValue(ENABLE_HTTP, object);
            object = mappedConfig.get(HTTPS_PORT);
            if(object != null) httpsPort = Config.loadIntegerValue(HTTPS_PORT, object);
            object = mappedConfig.get(ENABLE_HTTPS);
            if(object != null) enableHttps = Config.loadBooleanValue(ENABLE_HTTPS, object);
            object = mappedConfig.get(ENABLE_HTTP2);
            if(object != null) enableHttp2 = Config.loadBooleanValue(ENABLE_HTTP2, object);
            object = mappedConfig.get(KEYSTORE_NAME);
            if(object != null) keystoreName = (String)object;
            object = mappedConfig.get(KEYSTORE_PASS);
            if(object != null) keystorePass = (String)object;
            object = mappedConfig.get(KEY_PASS);
            if(object != null) keyPass = (String)object;
            object = mappedConfig.get(ENABLE_TWO_WAY_TLS);
            if(object != null) enableTwoWayTls = Config.loadBooleanValue(ENABLE_TWO_WAY_TLS, object);
            object = mappedConfig.get(TRUSTSTORE_NAME);
            if(object != null) truststoreName = (String)object;
            object = mappedConfig.get(TRUSTSTORE_PASS);
            if(object != null) truststorePass = (String)object;
            object = mappedConfig.get(ENABLE_REGISTRY);
            if(object != null) enableRegistry = Config.loadBooleanValue(ENABLE_REGISTRY, object);
            object = mappedConfig.get(SERVICE_ID);
            if(object != null) serviceId = (String)object;
            object = mappedConfig.get(SERVICE_NAME);
            if(object != null) serviceName = (String)object;
            object = mappedConfig.get(ENVIRONMENT);
            if(object != null) environment = (String)object;
            object = mappedConfig.get(BUILD_NUMBER);
            if(object != null) buildNumber = (String)object;
            object = mappedConfig.get(DYNAMIC_PORT);
            if(object != null) dynamicPort = Config.loadBooleanValue(DYNAMIC_PORT, object);
            object = mappedConfig.get(MIN_PORT);
            if(object != null) minPort = Config.loadIntegerValue(MIN_PORT, object);
            object = mappedConfig.get(MAX_PORT);
            if(object != null) maxPort = Config.loadIntegerValue(MAX_PORT, object);
            object = mappedConfig.get(BUFFER_SIZE);
            if(object != null) bufferSize = Config.loadIntegerValue(BUFFER_SIZE, object);
            object = mappedConfig.get(IO_THREADS);
            if(object != null) ioThreads = Config.loadIntegerValue(IO_THREADS, object);
            object = mappedConfig.get(WORKER_THREADS);
            if(object != null) workerThreads = Config.loadIntegerValue(WORKER_THREADS, object);
            object = mappedConfig.get(BACKLOG);
            if(object != null) backlog = Config.loadIntegerValue(BACKLOG, object);
            object = mappedConfig.get(SHUTDOWN_TIMEOUT);
            if(object != null) shutdownTimeout = Config.loadIntegerValue(SHUTDOWN_TIMEOUT, object);
            object = mappedConfig.get(SHUTDOWN_GRACEFUL_PERIOD);
            if(object != null) shutdownGracefulPeriod = Config.loadIntegerValue(SHUTDOWN_GRACEFUL_PERIOD, object);
            object = mappedConfig.get(ALWAYS_SET_DATE);
            if(object != null) alwaysSetDate = Config.loadBooleanValue(ALWAYS_SET_DATE, object);
            object = mappedConfig.get(ALLOW_UNESCAPED_CHARACTERS_IN_URL);
            if(object != null) allowUnescapedCharactersInUrl = Config.loadBooleanValue(ALLOW_UNESCAPED_CHARACTERS_IN_URL, object);
            object = mappedConfig.get(SERVER_STRING);
            if(object != null) serverString = (String)object;
            object = mappedConfig.get(BOOTSTRAP_STORE_NAME);
            if(object != null) bootstrapStoreName = (String)object;
            object = mappedConfig.get(BOOTSTRAP_STORE_PASS);
            if(object != null) bootstrapStorePass = (String)object;
            object = mappedConfig.get(MAX_TRANSFER_FILE_SIZE);
            if(object != null) maxTransferFileSize = Config.loadLongValue(MAX_TRANSFER_FILE_SIZE, object);
            object = mappedConfig.get(START_ON_REGISTRY_FAILURE);
            if(object != null) startOnRegistryFailure = Config.loadBooleanValue(START_ON_REGISTRY_FAILURE, object);
            object = mappedConfig.get(MASK_CONFIG_PROPERTIES);
            if(object != null) maskConfigProperties = Config.loadBooleanValue(MASK_CONFIG_PROPERTIES, object);
        }
    }

    public static ServerConfig getInstance() {
        if (instance == null) {
            synchronized (ServerConfig.class) {
                if (instance == null) {
                    instance = new ServerConfig();
                }
            }
        }
        return instance;
    }

    public static ServerConfig getInstance(String configName) {
        instance = new ServerConfig(configName);
        return instance;
    }

    /**
     * This method is not supposed to be used in production but only in testing.
     * @param configName String
     * @return ServerConfig object
     */
    public static ServerConfig get(String configName) {
        instance = new ServerConfig(configName);
        return instance;
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getHttpPort() {
    	String port = System.getProperty("httpPort");
    	if (port != null) {
    		try {
    			int newPort = Integer.parseInt(port);
    			httpPort = newPort;
    		}
    		catch (NumberFormatException ex) {
    			ex.printStackTrace(System.err);
    		}
    	}
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public boolean isEnableHttp() {
        return enableHttp;
    }

    public void setEnableHttp(boolean enableHttp) {
        this.enableHttp = enableHttp;
    }

    public int getHttpsPort() {
    	String port = System.getProperty("httpsPort");
    	if (port != null) {
    		try {
    			int newPort = Integer.parseInt(port);
    			httpsPort = newPort;
    		}
    		catch (NumberFormatException ex) {
    			ex.printStackTrace(System.err);
    		}
    	}
        return httpsPort;
    }

    public void setHttpsPort(int httpsPort) {
        this.httpsPort = httpsPort;
    }

    public boolean isEnableHttps() {
        return enableHttps;
    }

    public void setEnableHttps(boolean enableHttps) {
        this.enableHttps = enableHttps;
    }

    public String getKeystoreName() {
        return keystoreName;
    }

    public void setKeystoreName(String keystoreName) {
        this.keystoreName = keystoreName;
    }

    public String getTruststoreName() {
        return truststoreName;
    }

    public void setTruststoreName(String truststoreName) {
        this.truststoreName = truststoreName;
    }

    public String getKeystorePass() {
        return keystorePass;
    }

    public void setKeystorePass(String keystorePass) {
        this.keystorePass = keystorePass;
    }

    public String getKeyPass() {
        return keyPass;
    }

    public void setKeyPass(String keyPass) {
        this.keyPass = keyPass;
    }

    public String getTruststorePass() {
        return truststorePass;
    }

    public void setTruststorePass(String truststorePass) {
        this.truststorePass = truststorePass;
    }

    public boolean isEnableTwoWayTls() {
        return enableTwoWayTls;
    }

    public void setEnableTwoWayTls(boolean enableTwoWayTls) {
        this.enableTwoWayTls = enableTwoWayTls;
    }

    public boolean isEnableRegistry() {
        return enableRegistry;
    }

    public void setEnableRegistry(boolean enableRegistry) {
        this.enableRegistry = enableRegistry;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

    public boolean isEnableHttp2() {
        return enableHttp2;
    }

    public void setEnableHttp2(boolean enableHttp2) {
        this.enableHttp2 = enableHttp2;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    public boolean isDynamicPort() {
        return dynamicPort;
    }

    public void setDynamicPort(boolean dynamicPort) {
        this.dynamicPort = dynamicPort;
    }

    public int getMinPort() {
        return minPort;
    }

    public void setMinPort(int minPort) {
        this.minPort = minPort;
    }

    public int getMaxPort() {
        return maxPort;
    }

    public void setMaxPort(int maxPort) {
        this.maxPort = maxPort;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public int getIoThreads() {
        return ioThreads;
    }

    public void setIoThreads(int ioThreads) {
        this.ioThreads = ioThreads;
    }

    public int getWorkerThreads() {
        return workerThreads;
    }

    public void setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
    }

    public int getBacklog() {
        return backlog;
    }

    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }

    public int getShutdownTimeout() {
        return shutdownTimeout;
    }

    public void setShutdownTimeout(int shutdownTimeout) {
        this.shutdownTimeout = shutdownTimeout;
    }

    public int getShutdownGracefulPeriod() {
        return shutdownGracefulPeriod;
    }

    public void setShutdownGracefulPeriod(int shutdownGracefulPeriod) {
        this.shutdownGracefulPeriod = shutdownGracefulPeriod;
    }

    public boolean isAlwaysSetDate() {
        return alwaysSetDate;
    }

    public void setAlwaysSetDate(boolean alwaysSetDate) {
        this.alwaysSetDate = alwaysSetDate;
    }

    public String getServerString() {
        return serverString;
    }

    public void setServerString(String serverString) {
        this.serverString = serverString;
    }

    public boolean isAllowUnescapedCharactersInUrl() {
        return allowUnescapedCharactersInUrl;
    }

    public void setAllowUnescapedCharactersInUrl(boolean allowUnescapedCharactersInUrl) {
        this.allowUnescapedCharactersInUrl = allowUnescapedCharactersInUrl;
    }

    public String getBootstrapStoreName() {
        return bootstrapStoreName;
    }

    public void setBootstrapStoreName(String bootstrapStoreName) {
        this.bootstrapStoreName = bootstrapStoreName;
    }

    public String getBootstrapStorePass() {
        return bootstrapStorePass;
    }

    public void setBootstrapStorePass(String bootstrapStorePass) {
        this.bootstrapStorePass = bootstrapStorePass;
    }

    public long getMaxTransferFileSize() {
        return maxTransferFileSize;
    }

    public void setMaxTransferFileSize(long maxTransferFileSize) {
        this.maxTransferFileSize = maxTransferFileSize;
    }

    public boolean isStartOnRegistryFailure() {
        return startOnRegistryFailure;
    }

    public void setStartOnRegistryFailure(boolean startOnRegistryFailure) {
        this.startOnRegistryFailure = startOnRegistryFailure;
    }

    public boolean isMaskConfigProperties() {
        return maskConfigProperties;
    }

    public void setMaskConfigProperties(boolean maskConfigProperties) {
        this.maskConfigProperties = maskConfigProperties;
    }
}
