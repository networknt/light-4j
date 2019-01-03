package com.networknt.config;

import java.util.*;

public class CentralizedManagement {
    private static final String CENTRALIZED_MANAGEMENT = "values";
    private static final Map<String, Object> valueConfig = Config.getInstance().getJsonMapConfig(CENTRALIZED_MANAGEMENT);

    public static Map<String, Object> getValuesFromFile() {
        return valueConfig;
    }
}
