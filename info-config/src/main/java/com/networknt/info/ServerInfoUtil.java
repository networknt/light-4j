package com.networknt.info;

import com.networknt.config.Config;
import com.networknt.server.ServerConfig;
import com.networknt.utility.ConfigUtils;
import com.networknt.utility.FingerPrintUtil;
import com.networknt.utility.ModuleRegistry;
import com.networknt.utility.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.InetAddress;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.*;

public class ServerInfoUtil {
    static final Logger logger = LoggerFactory.getLogger(ServerInfoUtil.class);

    public static Map<String, Object> updateNormalizeKey(Map<String, Object> moduleRegistry, ServerInfoConfig config) {
        Map<String, Object> newModuleRegistry = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : moduleRegistry.entrySet()) {
            String key = entry.getKey();
            if (key.contains(":")) {
                key = key.substring(0, key.indexOf(":"));
            }
            newModuleRegistry.put(key, entry.getValue());
        }
        // normalized the key and value for comparison.
        newModuleRegistry = ConfigUtils.normalizeMap(newModuleRegistry, config.getKeysToNotSort());
        return newModuleRegistry;
    }
    public static Map<String, Object> getDeployment() {
        Map<String, Object> deploymentMap = new LinkedHashMap<>();
        deploymentMap.put("apiVersion", Util.getJarVersion());
        deploymentMap.put("frameworkVersion", getFrameworkVersion());
        return deploymentMap;
    }

    public static Map<String, Object> getEnvironment() {
        Map<String, Object> envMap = new LinkedHashMap<>();
        envMap.put("host", getHost());
        envMap.put("runtime", getRuntime());
        envMap.put("system", getSystem());
        return envMap;
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
    public static String getServerTlsFingerPrint() {
        String fingerPrint = null;
        ServerConfig serverConfig = ServerConfig.getInstance();
        // load keystore here based on server config and secret config
        String keystoreName = serverConfig.getKeystoreName();
        String keystorePass = serverConfig.getKeystorePass();
        if(keystoreName != null) {
            try (InputStream stream = Config.getInstance().getInputStreamFromFile(keystoreName)) {
                KeyStore loadedKeystore = KeyStore.getInstance("JKS");
                loadedKeystore.load(stream, keystorePass.toCharArray());
                X509Certificate cert = (X509Certificate)loadedKeystore.getCertificate(ServerConfig.CONFIG_NAME);
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

    public static Map<String, Object> getHost() {
        Map<String, Object> hostMap = new LinkedHashMap<>();
        String ip = "unknown";
        String hostname = "unknown";
        InetAddress inetAddress = Util.getInetAddress();
        ip = inetAddress.getHostAddress();
        hostname = inetAddress.getHostName();
        hostMap.put("ip", ip);
        hostMap.put("hostname", hostname);
        return hostMap;
    }

    public static Map<String, Object> getSecurity() {
        Map<String, Object> secMap = new LinkedHashMap<>();
        secMap.put("serverFingerPrint", getServerTlsFingerPrint());
        return secMap;
    }

    public static Map<String, Object> getServerInfo(ServerInfoConfig config) {
        Map<String, Object> infoMap = new LinkedHashMap<>();
        infoMap.put("deployment", getDeployment());
        infoMap.put("environment", getEnvironment());
        infoMap.put("security", getSecurity());
        // remove this as it is a rest specific. The specification is loaded in the specific handler.
        // infoMap.put("specification", Config.getInstance().getJsonMapConfigNoCache("openapi"));
        infoMap.put("component", updateNormalizeKey(ModuleRegistry.getModuleRegistry(), config));
        infoMap.put("plugin", updateNormalizeKey(ModuleRegistry.getPluginRegistry(), config));
        infoMap.put("plugins", ModuleRegistry.getPlugins());
        return infoMap;
    }

}
