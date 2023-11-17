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

package com.networknt.info;

import com.networknt.config.Config;
import com.networknt.handler.LightHttpHandler;
import com.networknt.security.IJwtVerifyHandler;
import com.networknt.security.JwtVerifier;
import com.networknt.utility.FingerPrintUtil;
import com.networknt.utility.ModuleRegistry;
import com.networknt.utility.Util;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.InetAddress;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * This is a server info handler that output the runtime info about the server. For example, how many
 * components are installed and what is the configuration of each component. For handlers, it is registered
 * when injecting into the handler chain during server startup. For other utilities, it should have a
 * static block to register itself during server startup. Additional info is gathered from environment
 * variable and JVM.
 *
 * @author Steve Hu
 */
public class ServerInfoGetHandler implements LightHttpHandler {

    static final String STATUS_SERVER_INFO_DISABLED = "ERR10013";

    static final Logger logger = LoggerFactory.getLogger(ServerInfoGetHandler.class);
    static ServerInfoConfig config;
    public ServerInfoGetHandler() {
        if(logger.isDebugEnabled()) logger.debug("ServerInfoGetHandler is constructed");
        config = (ServerInfoConfig)Config.getInstance().getJsonObjectConfig(ServerInfoConfig.CONFIG_NAME, ServerInfoConfig.class);
        ModuleRegistry.registerModule(ServerInfoConfig.CONFIG_NAME, ServerInfoConfig.class.getName(), Config.getInstance().getJsonMapConfigNoCache(ServerInfoConfig.CONFIG_NAME),null);
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if(config.isEnableServerInfo()) {
            Map<String,Object> infoMap = getServerInfo(exchange);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(infoMap));
        } else {
            setExchangeStatus(exchange, STATUS_SERVER_INFO_DISABLED);
        }
    }

    public static Map<String, Object> getServerInfo(final HttpServerExchange exchange) {
        Map<String, Object> infoMap = new LinkedHashMap<>();
        infoMap.put("deployment", getDeployment());
        infoMap.put("environment", getEnvironment(exchange));
        infoMap.put("security", getSecurity());
        // remove this as it is a rest specific. The specification is loaded in the specific handler.
        // infoMap.put("specification", Config.getInstance().getJsonMapConfigNoCache("openapi"));
        infoMap.put("component", updateKey(ModuleRegistry.getRegistry()));
        return infoMap;
    }

    public static Map<String, Object> updateKey (Map<String, Object> moduleRegistry) {
        Map<String, Object> newModuleRegistry = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : moduleRegistry.entrySet()) {
            String key = entry.getKey();
            if (key.contains(":")) {
                key = key.substring(0, key.indexOf(":"));
            }
            newModuleRegistry.put(key, entry.getValue());
        }
        return newModuleRegistry;
    }
    public static Map<String, Object> getDeployment() {
        Map<String, Object> deploymentMap = new LinkedHashMap<>();
        deploymentMap.put("apiVersion", Util.getJarVersion());
        deploymentMap.put("frameworkVersion", getFrameworkVersion());
        return deploymentMap;
    }

    public static Map<String, Object> getEnvironment(HttpServerExchange exchange) {
        Map<String, Object> envMap = new LinkedHashMap<>();
        envMap.put("host", getHost(exchange));
        envMap.put("runtime", getRuntime());
        envMap.put("system", getSystem());
        return envMap;
    }

    public static Map<String, Object> getSecurity() {
        Map<String, Object> secMap = new LinkedHashMap<>();
        // as we have replaced the static JwtHelper to JwtVerifier, we cannot use the static method to
        // get the fingerprints anymore, need to iterate the registered security module to do so.
        Map<String, Object> moduleMap = ModuleRegistry.getRegistry();
        // use set to eliminate duplications and will convert to ArrayList later
        Set<String> fingerprints = new HashSet<>();
        for(Object module: moduleMap.entrySet()) {
            if(module instanceof IJwtVerifyHandler) {
                IJwtVerifyHandler handler = (IJwtVerifyHandler)module;
                fingerprints.addAll(handler.getJwtVerifier().getFingerPrints());
            }
        }

        if(fingerprints.size() > 0) {
            secMap.put("oauth2FingerPrints", new ArrayList<String>(fingerprints));
        }
        secMap.put("serverFingerPrint", getServerTlsFingerPrint());
        return secMap;
    }

    public static Map<String, Object> getHost(HttpServerExchange exchange) {
        Map<String, Object> hostMap = new LinkedHashMap<>();
        String ip = "unknown";
        String hostname = "unknown";
        InetAddress inetAddress = Util.getInetAddress();
        ip = inetAddress.getHostAddress();
        hostname = inetAddress.getHostName();
        hostMap.put("ip", ip);
        hostMap.put("hostname", hostname);
        hostMap.put("dns", exchange.getSourceAddress().getHostName());
        return hostMap;
    }

    public static Map<String, Object> getRuntime() {
        Map<String, Object> runtimeMap = new LinkedHashMap<>();
        Runtime runtime = Runtime.getRuntime();
        runtimeMap.put("availableProcessors", runtime.availableProcessors());
        runtimeMap.put("freeMemory", runtime.freeMemory());
        runtimeMap.put("totalMemory", runtime.totalMemory());
        runtimeMap.put("maxMemory", runtime.maxMemory());
        return runtimeMap;
    }

    public static Map<String, Object> getSystem() {
        Map<String, Object> systemMap = new LinkedHashMap<>();
        Properties properties = System.getProperties();
        systemMap.put("javaVendor", properties.getProperty("java.vendor"));
        systemMap.put("javaVersion", properties.getProperty("java.version"));
        systemMap.put("osName", properties.getProperty("os.name"));
        systemMap.put("osVersion", properties.getProperty("os.version"));
        systemMap.put("userTimezone", properties.getProperty("user.timezone"));
        return systemMap;
    }

    public static String getFrameworkVersion() {
        String version = null;
        String path = "META-INF/maven/com.networknt/info/pom.properties";
        InputStream in = ClassLoader.getSystemResourceAsStream(path);
        try {
            Properties prop = new Properties();
            prop.load(in);
            version = prop.getProperty("version");
        } catch (Exception e) {
            //logger.error("Exception:", e);
        } finally {
            try { in.close(); }
            catch (Exception ignored){}
        }
        return version;
    }

    /**
     * We can get it from server module but we don't want mutual dependency. So
     * get it from config and keystore directly
     *
     * @return String TLS server certificate finger print
     */
    private static String getServerTlsFingerPrint() {
        String fingerPrint = null;
        Map<String, Object> serverConfig = Config.getInstance().getJsonMapConfigNoCache("server");
        // load keystore here based on server config and secret config
        String keystoreName = (String)serverConfig.get("keystoreName");
        String keystorePass = (String)serverConfig.get("keystorePass");
        if(keystoreName != null) {
            try (InputStream stream = Config.getInstance().getInputStreamFromFile(keystoreName)) {
                KeyStore loadedKeystore = KeyStore.getInstance("JKS");
                loadedKeystore.load(stream, keystorePass.toCharArray());
                X509Certificate cert = (X509Certificate)loadedKeystore.getCertificate("server");
                if(cert != null) {
                    fingerPrint = FingerPrintUtil.getCertFingerPrint(cert);
                } else {
                    logger.error("Unable to find the certificate with alias name as server in the keystore");
                }
            } catch (Exception e) {
                logger.error("Unable to load server keystore ", e);
            }
        }
        return fingerPrint;
    }
}
