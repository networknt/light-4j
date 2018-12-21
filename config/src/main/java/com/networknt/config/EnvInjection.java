package com.networknt.config;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnvInjection {
    private static Pattern pattern = Pattern.compile("\\$\\{(.*?)\\}");

    public static String inject(String string) {
        Matcher m = pattern.matcher(string);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            Object value = get(m.group(1));
            if (value == null) {
                throw new ConfigException(
                        m.group(1) + " appears in config file cannot be expanded");
            }
            m.appendReplacement(sb, (String)value);
        }
        return m.appendTail(sb).toString();
    }

    private static Object get(String content) {
        EnvEntity envEntity = getEnvEntity(content);
        Object value = null;
        if (envEntity != null) {
            value = getEnvVariable(envEntity.getEnvName());
            if (value == null || value.equals("")) {
                value = envEntity.getDefaultValue();
                if (value == null || value.equals("")) {
                    String error_text = envEntity.getErrorText();
                    if (error_text != null && !error_text.equals("")) {
                        throw new ConfigException(error_text);
                    }
                }
            }
        }
        return value;
    }

    private static EnvEntity getEnvEntity(String contents) {
        if (contents == null || contents.equals("")) {
            return null;
        }
        EnvEntity envEntity = new EnvEntity();
        contents = contents.trim();
        if (contents == null || contents.equals("")) {
            return null;
        }
        String[] array = contents.split(":", 2);
        if ("".equals(array[0])) {
            return null;
        }
        envEntity.setEnvName(array[0]);
        if (array.length == 2) {
            if (array[1].startsWith("?")) {
                envEntity.setErrorText(array[1].substring(1));
            }else if(array[1].startsWith("$")) {
                envEntity.setDefaultValue("\\$\\{" + array[0] + "\\}");
            }else {
                envEntity.setDefaultValue(array[1]);
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
}
