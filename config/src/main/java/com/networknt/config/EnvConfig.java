package com.networknt.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public class EnvConfig {
    static final Logger logger = LoggerFactory.getLogger(EnvConfig.class);

    public static void injectMapEnv(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String value = (String) entry.getValue();
            if (isEnvReference(value)) {
                EnvEntity envEntity = getEnvEntity(value);
                if (envEntity != null) {
                    String envName = envEntity.getEnvName();
                    Object envVariable = getEnvVariable(envName);
                    if (envVariable != null) {
                        map.put(entry.getKey(), envVariable);
                    } else if (envEntity.getDefaultValue() != null) {
                        map.put(entry.getKey(), envEntity.getDefaultValue());
                    } else if (envEntity.getErrorText() != null) {
                        logger.info(envEntity.getErrorText());
                        map.put(entry.getKey(), null);
                    } else {
                        logger.info("The environment variable:" + envName + " cannot be expanded.");
                        map.put(entry.getKey(), null);
                    }
                } else {
                    logger.info("The environment variable reference is empty.");
                    map.put(entry.getKey(), null);
                }
            } else if (isEscapeReference(value)) {
                map.put(entry.getKey(), null);
            }
        }
    }

    public static void injectObjectEnv(Object obj) {
        String[] fieldNames = getFieldName(obj);
        for (int i = 0; i < fieldNames.length; i++) {
            String value = (String) getFieldValueByName(fieldNames[i], obj);
            if (isEnvReference(value)) {
                EnvEntity envEntity = getEnvEntity(value);
                if (envEntity != null) {
                    String envName = envEntity.getEnvName();
                    Object envVariable = getEnvVariable(envName);
                    if (envVariable != null) {
                        setFieldValue(fieldNames[i], envVariable, obj);
                    } else if (envEntity.getDefaultValue() != null) {
                        setFieldValue(fieldNames[i], envEntity.getDefaultValue(), obj);
                    } else if (envEntity.getErrorText() != null) {
                        logger.info(envEntity.getErrorText());
                        setFieldValue(fieldNames[i], null, obj);
                    } else {
                        logger.info("The environment variable:" + envName + " cannot be expanded.");
                        setFieldValue(fieldNames[i], null, obj);
                    }
                } else {
                    logger.info("The environment variable reference is empty.");
                    setFieldValue(fieldNames[i], null, obj);
                }
            } else if (isEscapeReference(value)) {
                setFieldValue(fieldNames[i], null, obj);
            }
        }
    }

    private static EnvEntity getEnvEntity(String envReference) {
        String contents = getContents(envReference);
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

    private static boolean isEnvReference(String envName) {
        return envName.startsWith("${") && envName.endsWith("}");
    }

    private static boolean isEscapeReference(String envName) {
        return envName.startsWith("$${") && envName.endsWith("}");
    }

    private static String getContents(String envReference) {
        return envReference.substring(2, envReference.length() - 1);
    }

    private static String[] getFieldName(Object obj) {
        Field[] fields = obj.getClass().getDeclaredFields();
        String[] fieldName = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            fieldName[i] = fields[i].getName();
        }
        return fieldName;
    }

    private static Object getFieldValueByName(String fieldName, Object obj) {
        String getter = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        try {
            Method method = obj.getClass().getMethod(getter, new Class[]{});
            Object value = method.invoke(obj, new Object[]{});
            return value;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    private static void setFieldValue(String fieldName, Object fieldValue, Object obj) {
        String setter = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        String getter = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        try {
            Method method = obj.getClass().getMethod(setter, getReturnTypeFromGetterMethod(getter, obj));
            method.invoke(obj, fieldValue);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static Class<?> getReturnTypeFromGetterMethod(String getter, Object obj) {
        try {
            Method method = obj.getClass().getMethod(getter, new Class[]{});
            return method.getReturnType();
        } catch (NoSuchMethodException e) {
            logger.error(e.getMessage(), e);
            return null;
        }
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
