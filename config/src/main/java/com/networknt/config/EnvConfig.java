package com.networknt.config;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnvConfig {
    private static final String ENABLE_ENV_VARIABLE_INJECTION = "enable_env_variables_injection";
    private static Pattern pattern = Pattern.compile("[^/]\\$\\{(.*?)\\}(\")?");
    private static String enabled = System.getProperty(ENABLE_ENV_VARIABLE_INJECTION, "").toLowerCase();

    public static boolean isEnabled() {
        if ("false".equals(enabled)) {
            return false;
        } else {
            return true;
        }
    }

    public static InputStream resolveYaml(InputStream inStream) {
        String string = Config.convertStreamToString(inStream);
        Matcher m = pattern.matcher(string);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            Object variable = get(m.group(1));
            if (variable == null) {
                throw new ConfigException(
                        m.group(1) + " appears in config file cannot be expanded");
            }
            m.appendReplacement(sb, " " + (String) variable);
        }
        return convertStringToStream(m.appendTail(sb).toString());
    }

    public static InputStream resolveJson(InputStream inStream) {
        String string = Config.convertStreamToString(inStream);
        Matcher m = pattern.matcher(string);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            Object variable = get(m.group(1));
            if (variable == null) {
                throw new ConfigException(
                        m.group(1) + " appears in config file cannot be expanded");
            }
            m.appendReplacement(sb, "\"" + (String) variable + "\"");
        }
        return convertStringToStream(m.appendTail(sb).toString());
    }

    private static Object get(String content) {
        EnvEntity envEntity = getEnvEntity(content);
        Object envVariable = null;
        if (envEntity != null) {
            envVariable = getEnvVariable(envEntity.getEnvName());
            if (envVariable == null || envVariable.equals("")) {
                envVariable = envEntity.getDefaultValue();
                if (envVariable == null || envVariable.equals("")) {
                    String error_text = envEntity.getErrorText();
                    if (error_text != null && !error_text.equals("")) {
                        throw new ConfigException(error_text);
                    }
                }
            }
        }
        return envVariable;
    }

    private static EnvEntity getEnvEntity(String contents) {
        EnvEntity envEntity = new EnvEntity();
        contents = contents.trim();
        if (contents == null || contents.equals("")) {
            return null;
        }
        String[] rfcArray = contents.split(":", 2);
        if ("".equals(rfcArray[0])) {
            return null;
        }
        envEntity.setEnvName(rfcArray[0]);
        if (rfcArray.length == 2) {
            if (rfcArray[1].startsWith("?")) {
                envEntity.setErrorText(rfcArray[1].substring(1));
            } else {
                envEntity.setDefaultValue(rfcArray[1]);
            }
        }
        return envEntity;
    }

    private static Object getEnvVariable(String envName) {
        return System.getenv(envName);
    }

    private static class EnvEntity {
        private String envName;
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

        public String getEnvName() {
            return envName;
        }

        public void setEnvName(String envName) {
            this.envName = envName;
        }
    }

    private static InputStream convertStringToStream(String string) {
        return new ByteArrayInputStream(string.getBytes());
    }
}
