package com.networknt.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class has a public method called getInjectValue which is used to generate
 * the values which need to be injected from environment variables or a specific
 * file called "values.yaml".
 *
 * Three injection order defined as following and default mode is [2]:
 * [0] Inject from "values.yaml" only.
 * [1] Inject from system environment first, then overwrite by "values.yaml"
 * if exist.
 * [2] Inject from "values.yaml" first, then overwrite with the values in the
 * system environment.
 * This parameter can be set through setting system property "injection_order" in
 * commend line
 *
 * Created by jiachen on 2019-01-08.
 */
public class ConfigInjection {
    // Define the injection order
    private static final String INJECTION_ORDER = "injection_order";
    private static final String INJECTION_ORDER_CODE = (!System.getProperty(INJECTION_ORDER, "").equals("")) ?
            System.getProperty(INJECTION_ORDER, "") : "2";

    // Define one of the injection value source "values.yaml" and list of exclusion config files
    private static final String CENTRALIZED_MANAGEMENT = "values";
    private static final String EXCLUSIONS = "exclusions";

    private static final Map<String, Object> valueMap = Config.getInstance().getJsonMapConfig(CENTRALIZED_MANAGEMENT);
    private static final Map<String, Object> exclusionMap = Config.getInstance().getJsonMapConfig(EXCLUSIONS);

    // Define the injection pattern which represents the injection points
    private static Pattern pattern = Pattern.compile("\\$\\{(.*?)\\}");

    // Method used to generate the values from environment variables or "values.yaml"
    public static Object getInjectValue(String string) {
        Matcher m = pattern.matcher(string);
        StringBuffer sb = new StringBuffer();
        // Parse the content inside pattern "${}" when this pattern is found
        while (m.find()) {
            // Get parsing result
            Object value = getValue(m.group(1));
            // Throw exception when no parsing result found
            if (value == null) {
                throw new ConfigException(
                        m.group(1) + " appears in config file cannot be expanded");
            // Return directly when the parsing result don't need to be casted to String
            } else if (!(value instanceof String)) {
                return value;
            }
            m.appendReplacement(sb, (String) value);
        }
        return m.appendTail(sb).toString();
    }

    // Return the list of exclusion files list which includes the names of config files that shouldn't be injected
    // Double check values and exclusions to ensure no dead loop
    public static boolean isExclusionConfigFile(String fileName) {
        List<Object> exclusionConfigFileList = (exclusionMap == null) ? new ArrayList<>() : (List<Object>)exclusionMap.get("exclusionConfigFileList");
        return "values".equals(fileName.split("\\.")[0])
                || "exclusions".equals(fileName.split("\\.")[0])
                || exclusionConfigFileList.contains(fileName);
    }

    // Method used to parse the content inside pattern "${}"
    private static Object getValue(String content) {
        InjectionPattern injectionPattern = getInjectionPattern(content);
        Object value = null;
        if (injectionPattern != null) {
            // Use key of injectionPattern to get value from both environment variables and "values.yaml"
            Object envValue = System.getenv(injectionPattern.getKey());
            Object fileValue = (valueMap != null) ? valueMap.get(injectionPattern.getKey()) : null;
            // Return different value from different sources based on injection order defined before
            if (INJECTION_ORDER_CODE.equals("2") && envValue != null || (INJECTION_ORDER_CODE.equals("1") && fileValue == null)) {
                value = envValue;
            } else {
                value = fileValue;
            }
            // Return default value when no matched value found from environment variables and "values.yaml"
            if (value == null || value.equals("")) {
                value = injectionPattern.getDefaultValue();
                // Throw exception when error text provided
                if (value == null || value.equals("")) {
                    String error_text = injectionPattern.getErrorText();
                    if (error_text != null && !error_text.equals("")) {
                        throw new ConfigException(error_text);
                    }
                }
            }
        }
        return value;
    }

    // Get instance of InjectionPattern based on the contents inside pattern "${}"
    private static InjectionPattern getInjectionPattern(String contents) {
        if (contents == null || contents.trim().equals("")) {
            return null;
        }
        InjectionPattern injectionPattern = new InjectionPattern();
        contents = contents.trim();
        // Retrieve key, default value and error text
        String[] array = contents.split(":", 2);
        if ("".equals(array[0])) {
            return null;
        }
        // Set key of the injectionPattern
        injectionPattern.setKey(array[0]);
        if (array.length == 2) {
            // Set error text
            if (array[1].startsWith("?")) {
                injectionPattern.setErrorText(array[1].substring(1));
            // Skip this injection when "$" is found after the ":", and set "${key}" as default value
            } else if (array[1].startsWith("$")) {
                injectionPattern.setDefaultValue("\\$\\{" + array[0] + "\\}");
            // Set default value
            } else {
                injectionPattern.setDefaultValue(array[1]);
            }
        }
        return injectionPattern;
    }

    /**
     * Wrap the contents inside the pattern ${} into a private class which contains
     * three fields: key, defaultValue and errorText
      */
    private static class InjectionPattern {
        private String key;
        private String defaultValue;
        private String errorText;

        public String getErrorText() {
            return errorText;
        }

        public void setErrorText(String errorTest) {
            this.errorText = errorTest;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }
}
