package com.networknt.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CentralizedManagement {
    private static final String CENTRALIZED_MANAGEMENT = "values";
    private static final Map<String, Object> valueConfig = Config.getInstance().getJsonMapConfig(CENTRALIZED_MANAGEMENT);

    public static Map<String, Object> getValuesFromFile() {
        return valueConfig;
    }
}
