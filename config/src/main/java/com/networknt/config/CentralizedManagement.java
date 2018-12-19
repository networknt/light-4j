package com.networknt.config;

import org.jose4j.json.internal.json_simple.JSONValue;
import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.*;

public class CentralizedManagement {
    private static final String CENTRALIZED_MANAGEMENT = "values.yaml";
    private static final String ENABLE_CENTRALIZED_MANAGEMENT = "enable_centralized_management";
    private static final String LIGHT_4J_CONFIG_DIR = "light-4j-config-dir";
    private static final String EXTERNALIZED_PROPERTY_DIR = System.getProperty(LIGHT_4J_CONFIG_DIR, "");
    private static String enabled = System.getProperty(ENABLE_CENTRALIZED_MANAGEMENT, "").toLowerCase();

    static final Logger logger = LoggerFactory.getLogger(CentralizedManagement.class);

    public static boolean isEnabled() {
        if ("false".equals(enabled)) {
            return false;
        } else {
            return true;
        }
    }

    public String centralize(String configFullName, String targetConfig) {
        String valueConfig = null;
        String[] strings = configFullName.split("\\.");
        String configName = strings[0];
        String configType = strings[1];
        StringBuilder sb = new StringBuilder();
        try (InputStream inStream = getConfigStream(CENTRALIZED_MANAGEMENT)) {
            valueConfig = Config.convertStreamToString(inStream);
        } catch (IOException ioe) {
            logger.error("IOException", ioe);
        }
        String targetBlock = getTargetBlock(configName, valueConfig);
        if (targetBlock == null || targetBlock.equals("")) {
            return targetConfig;
        }
        if (configType.equals("yaml") || configType.equals("yml")) {
            sb.append(targetConfig);
            sb.append("\n" + targetBlock);
        } else {
            targetBlock = convertYamlToJson(targetBlock);
            while (targetConfig.endsWith("}") || targetConfig.endsWith("\n")) {
                targetConfig = targetConfig.substring(0,targetConfig.length() - 1);
            }
            sb.append(targetConfig + ",\n");
            sb.append(targetBlock.substring(1, targetBlock.length() - 1));
            sb.append("\n" + "}");
        }
        return sb.toString();
    }

    private String getTargetBlock(String configName, String valueConfig) {

        boolean read = false;
        StringBuilder targetBlock = new StringBuilder();
        String[] lines = valueConfig.split("\n");
        for (String line : lines) {
            if (line.equals(configName + ":")) {
                read = true;
            } else if (!line.startsWith(" ") && read) {
                return targetBlock.toString();
            } else if (read && line.length() > 4) {
                targetBlock.append(line.substring(4) + "\n");
            }
        }
        return targetBlock.toString();
    }

    private String convertYamlToJson(String yamlString) {
        Yaml yaml= new Yaml();
        Object obj = yaml.load(yamlString);

        return JSONValue.toJSONString(obj);
    }

    private InputStream getConfigStream(String configFilename) {
        InputStream inStream = null;
        try {
            inStream = new FileInputStream(EXTERNALIZED_PROPERTY_DIR + "/" + configFilename);
        } catch (FileNotFoundException ex) {
            if (logger.isInfoEnabled()) {
                logger.info("Unable to load config from externalized folder for " + Encode.forJava(configFilename + " in " + EXTERNALIZED_PROPERTY_DIR));
            }
        }
        if (inStream != null) {
            if (logger.isInfoEnabled()) {
                logger.info("Config loaded from externalized folder for " + Encode.forJava(configFilename + " in " + EXTERNALIZED_PROPERTY_DIR));
            }
            return inStream;
        }
        if (logger.isInfoEnabled()) {
            logger.info("Trying to load config from classpath directory for file " + Encode.forJava(configFilename));
        }
        inStream = getClass().getClassLoader().getResourceAsStream(configFilename);
        if (inStream != null) {
            if (logger.isInfoEnabled()) {
                logger.info("config loaded from classpath for " + Encode.forJava(configFilename));
            }
            return inStream;
        }
        inStream = getClass().getClassLoader().getResourceAsStream("config/" + configFilename);
        if (inStream != null) {
            if (logger.isInfoEnabled()) {
                logger.info("Config loaded from default folder for " + Encode.forJava(configFilename));
            }
            return inStream;
        }
        logger.info("Unable to load config " + Encode.forJava(configFilename) + ".");
        return null;
    }
}
