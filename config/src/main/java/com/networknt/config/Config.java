/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import com.networknt.config.yml.ConfigLoaderConstructor;
import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.networknt.config.yml.DecryptConstructor;
import com.networknt.config.yml.YmlConstants;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A injectable singleton config that has default implementation
 * based on FileSystem json files. It can be extended to
 * other sources (database, distributed cache etc.) by providing
 * another jar in the classpath to replace the default implementation.
 * <p>
 * Config files are loaded in the following sequence:
 * 1. resource/config folder for the default
 * 2. externalized directory specified by light-4j-config-dir
 * <p>
 * In docker, the config files should be in volume and any update will
 * be picked up the next day morning.
 */
public abstract class Config {
    public static final String LIGHT_4J_CONFIG_DIR = "light-4j-config-dir";

    protected Config() {
    }

    // abstract methods that need be implemented by all implementations

    public abstract Map<String, Object> getJsonMapConfig(String configName);

    public abstract Map<String, Object> getDefaultJsonMapConfig(String configName);

    public abstract Map<String, Object> getJsonMapConfig(String configName, String path);

    public abstract Map<String, Object> getDefaultJsonMapConfig(String configName, String path);

    public abstract Map<String, Object> getJsonMapConfigNoCache(String configName);

    public abstract Map<String, Object> getDefaultJsonMapConfigNoCache(String configName);

    public abstract Map<String, Object> getJsonMapConfigNoCache(String configName, String path);

    public abstract Map<String, Object> getDefaultJsonMapConfigNoCache(String configName, String path);

    public abstract Object getJsonObjectConfig(String configName, Class clazz);

    public abstract Object getDefaultJsonObjectConfig(String configName, Class clazz);

    public abstract Object getJsonObjectConfig(String configName, Class clazz, String path);

    public abstract Object getDefaultJsonObjectConfig(String configName, Class clazz, String path);

    public abstract String getStringFromFile(String filename);

    public abstract String getStringFromFile(String filename, String path);

    public abstract InputStream getInputStreamFromFile(String filename);

    public abstract ObjectMapper getMapper();

    public abstract Yaml getYaml();

    public abstract void clear();

    public abstract void setClassLoader(ClassLoader urlClassLoader);

    public abstract void putInConfigCache(String configName, Object config);

    public static Config getInstance() {
        return FileConfigImpl.DEFAULT;
    }

    private static final class FileConfigImpl extends Config {
    	static final String CONFIG_NAME = "config";
        static final String CONFIG_EXT_JSON = ".json";
        static final String CONFIG_EXT_YAML = ".yaml";
        static final String CONFIG_EXT_YML = ".yml";
        static final String[] configExtensionsOrdered = {CONFIG_EXT_YML, CONFIG_EXT_YAML, CONFIG_EXT_JSON};

        static final Logger logger = LoggerFactory.getLogger(Config.class);

        public final String[] EXTERNALIZED_PROPERTY_DIR = System.getProperty(LIGHT_4J_CONFIG_DIR, "").split(File.pathSeparator);

        private long cacheExpirationTime = 0L;

        private String configLoaderClass;

        private ConfigLoader configLoader;

        private ClassLoader classLoader;

        private static final Config DEFAULT = initialize();

        // Memory cache of all the configuration object. Each config will be loaded on the first time it is accessed.
        final Map<String, Object> configCache = new ConcurrentHashMap<>(10, 0.9f, 1);

        // An instance of Jackson ObjectMapper that can be used anywhere else for Json.
        final static ObjectMapper mapper = new ObjectMapper();

        static {
            mapper.registerModule(new JavaTimeModule());
            mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        }
        
        final Yaml yaml;
        
        FileConfigImpl(){
        	super();
        	String decryptorClass = getDecryptorClass();
        	configLoaderClass = getConfigLoaderClass();
        	if (null==decryptorClass || decryptorClass.trim().isEmpty()) {
        		yaml = new Yaml();
        	}else {
	            final Resolver resolver = new Resolver();
	            resolver.addImplicitResolver(YmlConstants.CRYPT_TAG, YmlConstants.CRYPT_PATTERN, YmlConstants.CRYPT_FIRST);
	        	yaml = new Yaml(new DecryptConstructor(decryptorClass), new Representer(), new DumperOptions(), resolver);
        	}
        }

        private static Config initialize() {
            Iterator<Config> it;
            it = ServiceLoader.load(Config.class).iterator();
            return it.hasNext() ? it.next() : new FileConfigImpl();
        }

        // Return instance of Jackson Object Mapper
        @Override
        public ObjectMapper getMapper() {
            return mapper;
        }

        @Override
        public Yaml getYaml() {
            return yaml;
        }

        @Override
        public void clear() {
            configCache.clear();
        }

        @Override
        public void setClassLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        private ClassLoader getClassLoader() {
            if (this.classLoader != null) {
                return this.classLoader;
            }
            return getClass().getClassLoader();
        }

        public void putInConfigCache(String configName, Object config) {
            configCache.put(configName,config);
        }

        @Override
        public String getStringFromFile(String filename, String path) {
            String content = (String) configCache.get(filename);
            if (content == null) {
                synchronized (FileConfigImpl.class) {
                    content = (String) configCache.get(filename);
                    if (content == null) {
                        content = loadStringFromFile(filename, path);
                        if (content != null) configCache.put(filename, content);
                    }
                }
            }
            return content;
        }

        @Override
        public String getStringFromFile(String filename) {
            return getStringFromFile(filename, "");
        }

        @Override
        public InputStream getInputStreamFromFile(String filename) {
            return getConfigStream(filename, "");
        }

        /**
         * Method used to load the configuration file as a given Object based on the config loader class configured in config.yml and cache it.
         * If no config loader is configured, file will be loaded by default loading method.
         * @param configName    The name of the config file, without an extension
         * @param clazz         The class that the object will be deserialized into
         * @param path          The relative directory or absolute directory that config will be loaded from
         * @return An instance of the object if possible, null otherwise. IOExceptions smothered.
         */
        @Override
        public Object getJsonObjectConfig(String configName, Class clazz, String path) {
            Object config = configCache.get(configName);
            if (config == null) {
                synchronized (FileConfigImpl.class) {
                    config = configCache.get(configName);
                    if (config == null) {
                        config = loadJsonObjectConfigWithSpecificConfigLoader(configName, clazz, path);
                        if (config != null) configCache.put(configName, config);
                    }
                }
            }
            return config;
        }

        /**
         * Method used to load the configuration file as a given Object by using default loading method and cache it.
         * @param configName    The name of the config file, without an extension
         * @param clazz         The class that the object will be deserialized into
         * @param path          The relative directory or absolute directory that config will be loaded from
         * @return An instance of the object if possible, null otherwise. IOExceptions smothered.
         */
        @Override
        public Object getDefaultJsonObjectConfig(String configName, Class clazz, String path) {
            Object config = configCache.get(configName);
            if (config == null) {
                synchronized (FileConfigImpl.class) {
                    config = configCache.get(configName);
                    if (config == null) {
                        config = loadObjectConfig(configName, clazz, path);
                        if (config != null) configCache.put(configName, config);
                    }
                }
            }
            return config;
        }

        /**
         * Method used to load the configuration file as a given Object based on the config loader class configured in config.yml and cache it.
         * If no config loader is configured, file will be loaded by default loading method.
         * @param configName    The name of the config file, without an extension
         * @param clazz         The class that the object will be deserialized into
         * @return An instance of the object if possible, null otherwise. IOExceptions smothered.
         */
        @Override
        public Object getJsonObjectConfig(String configName, Class clazz) {
            return getJsonObjectConfig(configName, clazz, "");
        }

        /**
         * Method used to load the configuration file as a given Object by using default loading method and cache it.
         * @param configName    The name of the config file, without an extension
         * @param clazz         The class that the object will be deserialized into
         * @return An instance of the object if possible, null otherwise. IOExceptions smothered.
         */
        @Override
        public Object getDefaultJsonObjectConfig(String configName, Class clazz) {
            return getDefaultJsonObjectConfig(configName, clazz, "");
        }

        /**
         * Method used to load the configuration file as a map based on the config loader class configured in config.yml and cache it.
         * If no config loader is configured, file will be loaded by default loading method.
         * @param configName    The name of the config file, without an extension
         * @param path          The relative directory or absolute directory that config will be loaded from
         * @return A map of the config fields if possible, null otherwise. IOExceptions smothered.
         */
        @Override
        public Map<String, Object> getJsonMapConfig(String configName, String path) {
            Map<String, Object> config = (Map<String, Object>) configCache.get(configName);
            if (config == null) {
                synchronized (FileConfigImpl.class) {
                    config = (Map<String, Object>) configCache.get(configName);
                    if (config == null) {
                        config = loadJsonMapConfigWithSpecificConfigLoader(configName, path);
                        if (config != null) configCache.put(configName, config);
                    }
                }
            }
            return config;
        }

        /**
         * Method used to load the configuration file as a map by using default loading method and cache it.
         * @param configName    The name of the config file, without an extension
         * @param path          The relative directory or absolute directory that config will be loaded from
         * @return A map of the config fields if possible, null otherwise. IOExceptions smothered.
         */
        @Override
        public Map<String, Object> getDefaultJsonMapConfig(String configName, String path) {
            Map<String, Object> config = (Map<String, Object>) configCache.get(configName);
            if (config == null) {
                synchronized (FileConfigImpl.class) {
                    config = (Map<String, Object>) configCache.get(configName);
                    if (config == null) {
                        config = loadMapConfig(configName, path);
                        if (config != null) configCache.put(configName, config);
                    }
                }
            }
            return config;
        }

        /**
         * Method used to load the configuration file as a map based on the config loader class configured in config.yml and cache it.
         * If no config loader is configured, file will be loaded by default loading method.
         * @param configName    The name of the config file, without an extension
         * @return A map of the config fields if possible, null otherwise. IOExceptions smothered.
         */
        @Override
        public Map<String, Object> getJsonMapConfig(String configName) {
            return getJsonMapConfig(configName, "");
        }

        /**
         * Method used to load the configuration file as a map by using default loading method and cache it.
         * @param configName    The name of the config file, without an extension
         * @return A map of the config fields if possible, null otherwise. IOExceptions smothered.
         */
        @Override
        public Map<String, Object> getDefaultJsonMapConfig(String configName) {
            return getDefaultJsonMapConfig(configName, "");
        }

        /**
         * Method used to load the configuration file as a map based on the config loader class configured in config.yml without caching.
         * If no config loader is configured, file will be loaded by default loading method.
         * @param configName    The name of the config file, without an extension
         * @param path          The relative directory or absolute directory that config will be loaded from
         * @return A map of the config fields if possible, null otherwise. IOExceptions smothered.
         */
        @Override
        public Map<String, Object> getJsonMapConfigNoCache(String configName, String path) {
            return loadJsonMapConfigWithSpecificConfigLoader(configName, path);
        }

        /**
         * Method used to load the configuration file as a map by using default loading method without caching.
         * @param configName    The name of the config file, without an extension
         * @param path          The relative directory or absolute directory that config will be loaded from
         * @return A map of the config fields if possible, null otherwise. IOExceptions smothered.
         */
        @Override
        public Map<String, Object> getDefaultJsonMapConfigNoCache(String configName, String path) {
            return loadMapConfig(configName, path);
        }

        /**
         * Method used to load the configuration file as a map based on the config loader class configured in config.yml without caching.
         * If no config loader is configured, file will be loaded by default loading method.
         * @param configName    The name of the config file, without an extension
         * @return A map of the config fields if possible, null otherwise. IOExceptions smothered.
         */
        @Override
        public Map<String, Object> getJsonMapConfigNoCache(String configName) {
            return getJsonMapConfigNoCache(configName, "");
        }

        /**
         * Method used to load the configuration file as a map by using default loading method without caching.
         * @param configName    The name of the config file, without an extension
         * @return A map of the config fields if possible, null otherwise. IOExceptions smothered.
         */
        @Override
        public Map<String, Object> getDefaultJsonMapConfigNoCache(String configName) {
            return getDefaultJsonMapConfigNoCache(configName, "");
        }

        private String loadStringFromFile(String filename, String path) {
            String content = null;
            InputStream inStream = null;
            try {
                inStream = getConfigStream(filename, path);
                if (inStream != null) {
                    content = convertStreamToString(inStream);
                }
            } catch (Exception ioe) {
                logger.error("Exception", ioe);
            } finally {
                if (inStream != null) {
                    try {
                        inStream.close();
                    } catch (IOException ioe) {
                        logger.error("IOException", ioe);
                    }
                }
            }
            return content;
        }

        /**
         * Helper method to reduce duplication of loading a given file as a given Object.
         * @param configName    The name of the config file, without an extension
         * @param fileExtension The extension (with a leading .)
         * @param clazz         The class that the object will be deserialized into.
         * @param <T>           The type of the class file should be the type of the object returned.
         * @param path          The relative directory or absolute directory that config will be loaded from
         * @return An instance of the object if possible, null otherwise. IOExceptions smothered.
         */
        private <T> Object loadSpecificConfigFileAsObject(String configName, String fileExtension, Class<T> clazz, String path) {
            Object config = null;
            String fileName = configName + fileExtension;
            try (InputStream inStream = getConfigStream(fileName, path)) {
                if (inStream != null) {
                    // The config file specified in the config.yml shouldn't be injected
                    if (ConfigInjection.isExclusionConfigFile(configName)) {
                        config = yaml.loadAs(inStream, clazz);
                    } else {
                        // Parse into map first, since map is easier to be manipulated in merging process
                        Map<String, Object> configMap = yaml.load(inStream);
                        config = CentralizedManagement.mergeObject(configMap, clazz);
                    }
                }
            } catch (Exception e) {
                logger.error("Exception", e);
                throw new RuntimeException("Unable to load " + fileName + " as object.", e);
            }
            return config;
        }

        private <T> Object loadObjectConfig(String configName, Class<T> clazz, String path) {
            Object config;
            for (String extension : configExtensionsOrdered) {
                config = loadSpecificConfigFileAsObject(configName, extension, clazz, path);
                if (config != null) return config;
            }
            return null;
        }

        /**
         * Helper method to reduce duplication of loading a given config file as a Map.
         * @param configName    The name of the config file, without an extension
         * @param fileExtension The extension (with a leading .)
         * @param path          The relative directory or absolute directory that config will be loaded from
         * @return A map of the config fields if possible, null otherwise. IOExceptions smothered.
         */
        private Map<String, Object> loadSpecificConfigFileAsMap(String configName, String fileExtension, String path) {
            Map<String, Object> config = null;
            String ymlFilename = configName + fileExtension;
            try (InputStream inStream = getConfigStream(ymlFilename, path)) {
                if (inStream != null) {
                    config = yaml.load(inStream);
                    if (!ConfigInjection.isExclusionConfigFile(configName)) {
                        CentralizedManagement.mergeMap(config); // mutates the config map in place.
                    }
                }
            } catch (Exception e) {
                logger.error("Exception", e);
                throw new RuntimeException("Unable to load " + ymlFilename + " as map.", e);
            }
            return config;
        }
        
        
        private Map<String, Object> loadMapConfig(String configName, String path) {
            Map<String, Object> config;
            for (String extension : configExtensionsOrdered) {
                config = loadSpecificConfigFileAsMap(configName, extension, path);
                if (config != null) return config;
            }
            return null;
        }

        private InputStream getConfigStream(String configFilename, String path) {

            InputStream inStream = null;
            String configFileDir = null;
            for (int i = 0; i < EXTERNALIZED_PROPERTY_DIR.length; i ++) {
                String absolutePath = getAbsolutePath(path, i);
                try {
                    inStream = new FileInputStream(absolutePath + "/" + configFilename);
                    configFileDir = absolutePath;
                } catch (FileNotFoundException ex) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Unable to load config from externalized folder for " + Encode.forJava(configFilename + " in " + absolutePath));
                    }
                }
                // absolute path do not need to continue
                if (path.startsWith("/")) break;
            }
            if (inStream != null) {
                if (logger.isInfoEnabled()) {
                    logger.info("Config loaded from externalized folder for " + Encode.forJava(configFilename + " in " + configFileDir));
                }
                return inStream;
            }
            if (logger.isInfoEnabled()) {
                logger.info("Trying to load config from classpath directory for file " + Encode.forJava(configFilename));
            }
            inStream = this.getClassLoader().getResourceAsStream(configFilename);
            if (inStream != null) {
                if (logger.isInfoEnabled()) {
                    logger.info("config loaded from classpath for " + Encode.forJava(configFilename));
                }
                return inStream;
            }
            inStream = this.getClassLoader().getResourceAsStream("config/" + configFilename);
            if (inStream != null) {
                if (logger.isInfoEnabled()) {
                    logger.info("Config loaded from default folder for " + Encode.forJava(configFilename));
                }
                return inStream;
            }
            if (configFilename.endsWith(CONFIG_EXT_YML)) {
                logger.info("Unable to load config " + Encode.forJava(configFilename) + ". Looking for the same file name with extension yaml...");
            } else if (configFilename.endsWith(CONFIG_EXT_YAML)) {
                logger.info("Unable to load config " + Encode.forJava(configFilename) + ". Looking for the same file name with extension json...");
            } else if (configFilename.endsWith(CONFIG_EXT_JSON)) {
                System.out.println("Unable to load config '" + Encode.forJava(configFilename.substring(0, configFilename.indexOf("."))) + "' with extension yml, yaml and json from external config, application config and module config. Please ignore this message if you are sure that your application is not using this config file.");
            }
            return null;
        }

        private static long getNextMidNightTime() {
            Calendar cal = new GregorianCalendar();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            cal.add(Calendar.DAY_OF_MONTH, 1);
            return cal.getTimeInMillis();
        }

        // private method used to get absolute directory, input path can be absolute or relative
        private String getAbsolutePath(String path, int index) {
            if (path.startsWith("/")) {
                return path;
            } else {
                return path.equals("") ? EXTERNALIZED_PROPERTY_DIR[index].trim() : EXTERNALIZED_PROPERTY_DIR[index].trim() + "/" + path;
            }
        }

        private String getDecryptorClass() {
            Map<String, Object> config = loadModuleConfig();
            if (null != config) {
                String decryptorClass = (String) config.get(DecryptConstructor.CONFIG_ITEM_DECRYPTOR_CLASS);
                if (logger.isDebugEnabled()) {
                    logger.debug("found decryptorClass={}", decryptorClass);
                }
                return decryptorClass == null ? DecryptConstructor.DEFAULT_DECRYPTOR_CLASS : decryptorClass;
            }else {
                logger.warn("config file cannot be found.");
            }
            return DecryptConstructor.DEFAULT_DECRYPTOR_CLASS;
        }

        private String getConfigLoaderClass() {
            Map<String, Object> config = loadModuleConfig();
            if (null != config) {
                String configLoaderClass = (String) config.get(ConfigLoaderConstructor.CONFIG_LOADER_CLASS);
                if (logger.isDebugEnabled()) {
                    logger.debug("found configLoaderClass={}", configLoaderClass);
                }
                return configLoaderClass;
            }else {
                logger.warn("config file cannot be found.");
            }
            return null;
        }

        private Map<String, Object> loadModuleConfigNoCache() {
            Yaml yml = new Yaml();

            Map<String, Object> config = null;
            for (String extension : configExtensionsOrdered) {
                String ymlFilename = CONFIG_NAME + extension;
                try (InputStream inStream = getConfigStream(ymlFilename, "")) {
                    if (inStream != null) {
                        config = yml.load(inStream);
                    }
                } catch (IOException ioe) {
                    logger.error("IOException", ioe);
                }

                if (config != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("loaded config from file {}", ymlFilename);
                    }

                    break;
                }
            }
            return config;
        }

        private Map<String, Object> loadModuleConfig() {
            Map<String, Object> config = (Map<String, Object>) configCache.get(CONFIG_NAME);
            if (config == null) {
                synchronized (FileConfigImpl.class) {
                    config = (Map<String, Object>) configCache.get(CONFIG_NAME);
                    if (config == null) {
                        config = loadModuleConfigNoCache();
                        if (config != null) configCache.put(CONFIG_NAME, config);
                    }
                }
            }
            return config;
        }

        private Map<String, Object> loadJsonMapConfigWithSpecificConfigLoader(String configName, String path) {
            Map<String, Object> config = null;
            // Initialize config loader
            if (configLoaderClass != null && this.configLoader == null) {
                this.configLoader = new ConfigLoaderConstructor(configLoaderClass).getConfigLoader();
            }
            if (configLoader != null) {
                logger.info("Trying to load {} with extension yaml, yml or json by using ConfigLoader: {}.", configName, configLoader.getClass().getName());
                if (path == null || path.equals("")) {
                    config = configLoader.loadMapConfig(configName);
                } else {
                    config = configLoader.loadMapConfig(configName, path);
                }
            }
            // Fall back to default loading method if the configuration cannot be loaded by specific config loader
            if (config == null) {
                logger.info("Trying to load {} with extension yaml, yml or json by using default loading method.", configName);
                config = loadMapConfig(configName, path);
            }
            return config;
        }

        private Object loadJsonObjectConfigWithSpecificConfigLoader(String configName, Class clazz, String path) {
            Object config = null;
            // Initialize config loader
            if (configLoaderClass != null && this.configLoader == null) {
                this.configLoader = new ConfigLoaderConstructor(configLoaderClass).getConfigLoader();
            }
            if (this.configLoader != null) {
                logger.info("Trying to load {} with extension yaml, yml or json by using ConfigLoader: {}.", configName, configLoader.getClass().getName());
                if (path == null || path.equals("")) {
                    config = configLoader.loadObjectConfig(configName, clazz);
                } else {
                    config = configLoader.loadObjectConfig(configName, clazz, path);
                }
            }
            // Fall back to default loading method if the configuration cannot be loaded by specific config loader
            if (config == null) {
                logger.info("Trying to load {} with extension yaml, yml or json by using default loading method.", configName);
                config = loadObjectConfig(configName, clazz, path);
            }
            return config;
        }
    }

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    static InputStream convertStringToStream(String string) {
        return new ByteArrayInputStream(string.getBytes(UTF_8));
    }
}

