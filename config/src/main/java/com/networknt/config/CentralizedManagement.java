package com.networknt.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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


}
