package com.networknt.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class EnvConfig {
    static final Logger logger = LoggerFactory.getLogger(EnvConfig.class);

    private static boolean[] escape;

    public static InputStream preprocessYaml(String string) {
        String[] lines = string.split("\n");
        escape = new boolean[lines.length];
        for (int i = 0; i < lines.length; i++) {
            if ((lines[i].contains(": ${") || lines[i].contains("- ${")) && lines[i].endsWith("}")) {
                int start = lines[i].indexOf(": ${");
                if (start == -1) start = lines[i].indexOf("- ${");
                int index = start + 4;
                StringBuilder stringBuilder = new StringBuilder();
                while (index < lines[i].length() - 1) {
                    stringBuilder.append(lines[i].charAt(index));
                    index++;
                }
                lines[i] = lines[i].substring(0, start + 2) + get(stringBuilder.toString(), i);
            } else if ((lines[i].contains(": $${") || lines[i].contains("- $${")) && lines[i].endsWith("}")) {
                escape[i] = true;
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (!escape[i]) {
                stringBuilder.append(lines[i] + "\n");
            }
        }
        return convertStringToStream(stringBuilder.substring(0, stringBuilder.length() - 1));
    }

    public static InputStream preprocessJson(String string) {
        String[] lines = string.split(",");
        escape = new boolean[lines.length];
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(": \"${") && (lines[i].endsWith("}\""))) {
                int start = lines[i].indexOf(": \"${");
                int index = start + 5;
                StringBuilder stringBuilder = new StringBuilder();
                while (index < lines[i].length() - 2) {
                    stringBuilder.append(lines[i].charAt(index));
                    index++;
                }
                lines[i] = lines[i].substring(0, start + 3) + get(stringBuilder.toString(), i) + "\"";
            } else if (lines[i].contains(": \"$${") && (lines[i].endsWith("}\"\n}") || lines[i].endsWith("}\"}"))) {
                escape[i] = true;
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (!escape[i]) {
                stringBuilder.append(lines[i] + ",");
            }
        }
        if (stringBuilder.length() == 0) {
            return convertStringToStream("{}");
        }
        String result = stringBuilder.substring(0, stringBuilder.length() - 1);
        if (!result.endsWith("}")) {
            result = result + "}";
        }
        return convertStringToStream(result);
    }

    private static Object get(String content, int index) {
        EnvEntity envEntity = getEnvEntity(content);
        Object envVariable = null;
        if (envEntity != null) {
            envVariable = getEnvVariable(envEntity.getEnvName());
            if (envVariable == null || envVariable.equals("")) {
                envVariable = envEntity.getDefaultValue();
                if (envVariable == null || envVariable.equals("")) {
                    String error_text = envEntity.getErrorText();
                    escape[index] = true;
                    if (error_text != null && !error_text.equals("")) {
                        logger.error(error_text);
                    }
                }
            }
        }
        return envVariable;
    }

    private static EnvEntity getEnvEntity(String contents) {
        EnvEntity envEntity = new EnvEntity();
        if (contents == null || contents.equals("")) {
            return null;
        }
        String[] rfcArray = contents.split(":", 2);
        rfcArray[0] = rfcArray[0].trim();
        if ("".equals(rfcArray[0])) {
            return null;
        }
        envEntity.setEnvName(rfcArray[0]);
        if (rfcArray.length == 2) {
            rfcArray[1] = rfcArray[1].trim();
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
        private String key;
        private String envName;
        private String defaultValue;
        private String errorText;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

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
