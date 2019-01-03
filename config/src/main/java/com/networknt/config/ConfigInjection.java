package com.networknt.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigInjection {
    private static final String INJECTION_ORDER = "injection_order";
    private static final String INJECTION_ORDER_CODE = (!System.getProperty(INJECTION_ORDER, "").equals("")) ?
            System.getProperty(INJECTION_ORDER, "") : "2";

    private static final String CENTRALIZED_MANAGEMENT = "values";
    private static final Map<String, Object> valueMap = Config.getInstance().getJsonMapConfig(CENTRALIZED_MANAGEMENT);

    static final Logger logger = LoggerFactory.getLogger(ConfigInjection.class);

    private static Pattern pattern = Pattern.compile("\\$\\{(.*?)\\}");

    public static String inject(String string) {
        Matcher m = pattern.matcher(string);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            Object value = getValue(m.group(1));
            if (value == null) {
                throw new ConfigException(
                        m.group(1) + " appears in config file cannot be expanded");
            }
            if (value instanceof Integer) {
                value = String.valueOf(value);
            }
            m.appendReplacement(sb, (String) value);
        }
        return m.appendTail(sb).toString();
    }

    private static Object getValue(String content) {
        InjectionPattern injectionPattern = getInjectionPattern(content);
        Object value = null;
        if (injectionPattern != null) {
            Object envValue = System.getenv(injectionPattern.getKey());
            Object fileValue = (valueMap != null) ? valueMap.get(injectionPattern.getKey()) : null;
            if (INJECTION_ORDER_CODE.equals("2") && envValue != null || (INJECTION_ORDER_CODE.equals("1") && fileValue == null)) {
                value = envValue;
            } else {
                value = fileValue;
            }
            if (value == null || value.equals("")) {
                value = injectionPattern.getDefaultValue();
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

    private static InjectionPattern getInjectionPattern(String contents) {
        if (contents == null || contents.trim().equals("")) {
            return null;
        }
        InjectionPattern injectionPattern = new InjectionPattern();
        contents = contents.trim();
        String[] array = contents.split(":", 2);
        if ("".equals(array[0])) {
            return null;
        }
        injectionPattern.setKey(array[0]);
        if (array.length == 2) {
            if (array[1].startsWith("?")) {
                injectionPattern.setErrorText(array[1].substring(1));
            } else if (array[1].startsWith("$")) {
                injectionPattern.setDefaultValue("\\$\\{" + array[0] + "\\}");
            } else {
                injectionPattern.setDefaultValue(array[1]);
            }
        }
        return injectionPattern;
    }

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
