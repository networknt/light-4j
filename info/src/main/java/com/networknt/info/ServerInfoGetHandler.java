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

package com.networknt.info;

import com.networknt.config.Config;
import com.networknt.security.JwtHelper;
import com.networknt.status.Status;
import com.networknt.utility.FingerPrintUtil;
import com.networknt.utility.ModuleRegistry;
import com.networknt.utility.Util;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.InetAddress;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * This is a server info handler that output the runtime info about the server. For example, how many
 * components are installed and what is the configuration of each component. For handlers, it is registered
 * when injecting into the handler chain during server startup. For other utilities, it should have a
 * static block to register itself during server startup. Additional info is gathered from environment
 * variable and JVM.
 *
 * @author Steve Hu
 */
public class ServerInfoGetHandler implements HttpHandler {
    public static final String CONFIG_NAME = "info";

    static final String STATUS_SERVER_INFO_DISABLED = "ERR10013";

    static final Logger logger = LoggerFactory.getLogger(ServerInfoGetHandler.class);

    public ServerInfoGetHandler(){}

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        ServerInfoConfig config = (ServerInfoConfig)Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ServerInfoConfig.class);
        if(config.isEnableServerInfo()) {
            Map<String, Object> infoMap = new LinkedHashMap<>();
            infoMap.put("deployment", getDeployment());
            infoMap.put("environment", getEnvironment(exchange));
            infoMap.put("security", getSecurity());
            infoMap.put("specification", Config.getInstance().getJsonMapConfigNoCache("swagger"));
            infoMap.put("component", ModuleRegistry.getRegistry());
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(infoMap));
        } else {
            Status status = new Status(STATUS_SERVER_INFO_DISABLED);
            exchange.setStatusCode(status.getStatusCode());
            exchange.getResponseSender().send(status.toString());
        }
    }

    public Map<String, Object> getDeployment() {
        Map<String, Object> deploymentMap = new LinkedHashMap<>();
        deploymentMap.put("apiVersion", Util.getJarVersion());
        deploymentMap.put("frameworkVersion", getFrameworkVersion());
        return deploymentMap;
    }

    public Map<String, Object> getEnvironment(HttpServerExchange exchange) {
        Map<String, Object> envMap = new LinkedHashMap<>();
        envMap.put("host", getHost(exchange));
        envMap.put("runtime", getRuntime());
        envMap.put("system", getSystem());
        return envMap;
    }

    public Map<String, Object> getSecurity() {
        Map<String, Object> secMap = new LinkedHashMap<>();
        secMap.put("oauth2FingerPrints", JwtHelper.getFingerPrints());
        secMap.put("serverFingerPrint", getServerTlsFingerPrint());
        return secMap;
    }

    public Map<String, Object> getHost(HttpServerExchange exchange) {
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

    public Map<String, Object> getRuntime() {
        Map<String, Object> runtimeMap = new LinkedHashMap<>();
        Runtime runtime = Runtime.getRuntime();
        runtimeMap.put("availableProcessors", runtime.availableProcessors());
        runtimeMap.put("freeMemory", runtime.freeMemory());
        runtimeMap.put("totalMemory", runtime.totalMemory());
        runtimeMap.put("maxMemory", runtime.maxMemory());
        return runtimeMap;
    }

    public Map<String, Object> getSystem() {
        Map<String, Object> systemMap = new LinkedHashMap<>();
        Properties properties = System.getProperties();
        systemMap.put("javaVendor", properties.getProperty("java.vendor"));
        systemMap.put("javaVersion", properties.getProperty("java.version"));
        systemMap.put("osName", properties.getProperty("os.name"));
        systemMap.put("osVersion", properties.getProperty("os.version"));
        systemMap.put("userTimezone", properties.getProperty("user.timezone"));
        return systemMap;
    }

    public String getFrameworkVersion() {
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
    private String getServerTlsFingerPrint() {
        String fingerPrint = null;
        Map<String, Object> serverConfig = Config.getInstance().getJsonMapConfigNoCache("server");
        Map<String, Object> secretConfig = Config.getInstance().getJsonMapConfigNoCache("secret");
        // load keystore here based on server config and secret config
        String keystoreName = (String)serverConfig.get("keystoreName");
        String serverKeystorePass = (String)secretConfig.get("serverKeystorePass");
        if(keystoreName != null) {
            try (InputStream stream = Config.getInstance().getInputStreamFromFile(keystoreName)) {
                KeyStore loadedKeystore = KeyStore.getInstance("JKS");
                loadedKeystore.load(stream, serverKeystorePass.toCharArray());
                X509Certificate cert = (X509Certificate)loadedKeystore.getCertificate("server");
                fingerPrint = FingerPrintUtil.getCertFingerPrint(cert);
            } catch (Exception e) {
                logger.error("Unable to load server keystore ", e);
            }
        }
        return fingerPrint;
    }

}
