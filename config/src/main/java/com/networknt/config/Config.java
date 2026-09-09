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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.networknt.config.yml.ConfigLoaderConstructor;
import com.networknt.config.yml.DecryptConstructor;
import com.networknt.config.yml.YmlConstants;
import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.networknt.config.ConfigInjection.CENTRALIZED_MANAGEMENT;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * An injectable singleton config that has default implementation
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
    /** Constant for the configuration directory property name */
    public static final String LIGHT_4J_CONFIG_DIR = "light-4j-config-dir";


    /** Default constructor for Config */
    protected Config() {
    }

    // abstract methods that need be implemented by all implementations

    /**
     * Gets a JSON map configuration by name.
     *
     * @param configName name of the configuration
     * @return Map of configuration values
     */
    public abstract Map<String, Object> getJsonMapConfig(String configName);


    /**
     * Gets a default JSON map configuration by name.
     *
     * @param configName name of the configuration
     * @return Map of configuration values
     */
    public abstract Map<String, Object> getDefaultJsonMapConfig(String configName);


    public abstract Map<String, Object> getJsonMapConfig(String configName, String path);

    public abstract Map<String, Object> getDefaultJsonMapConfig(String configName, String path);

    public abstract Map<String, Object> getJsonMapConfigNoCache(String configName);

    public abstract Map<String, Object> getDefaultJsonMapConfigNoCache(String configName);

    public abstract Map<String, Object> getJsonMapConfigNoCache(String configName, String path);

    public abstract Map<String, Object> getDefaultJsonMapConfigNoCache(String configName, String path);

    public abstract Object getJsonObjectConfig(String configName, Class clazz);
    public abstract Object getJsonObjectConfigNoCache(String configName, Class clazz);

    public abstract Object getDefaultJsonObjectConfig(String configName, Class clazz);

    public abstract Object getJsonObjectConfig(String configName, Class clazz, String path);

    public abstract Object getDefaultJsonObjectConfig(String configName, Class clazz, String path);

    public abstract String getStringFromFile(String filename);

    public abstract String getStringFromFile(String filename, String path);

    public abstract InputStream getInputStreamFromFile(String filename);

    public abstract ObjectMapper getMapper();

    public abstract Yaml getYaml();

    public abstract boolean isDecrypt();

    public abstract void clear();

    public abstract void clearConfigCache(String configName);

    public abstract void setClassLoader(ClassLoader urlClassLoader);

    public abstract void putInConfigCache(String configName, Object config);

    /**
     * Gets the singleton instance of Config.
     *
     * @return Config instance
     */
    public static Config getInstance() {
        return FileConfigImpl.DEFAULT;
    }
    /**
     * Gets a non-decrypted instance of Config.
     *
     * @return Config instance
     */
    public static Config getNoneDecryptedInstance() {
        return NoneDecryptedConfigImpl.NONE_DECRYPTED;
    }

    // public abstract String getDecryptorClassPublic();

    private static abstract class AbstractConfigImpl extends Config {
        static final String CONFIG_NAME = "config";
        static final String CONFIG_EXT_JSON = ".json";
        static final String CONFIG_EXT_YAML = ".yaml";
        static final String CONFIG_EXT_YML = ".yml";
        static final String[] configExtensionsOrdered = {CONFIG_EXT_YML, CONFIG_EXT_YAML, CONFIG_EXT_JSON};
        static final Logger logger = LoggerFactory.getLogger(NoneDecryptedConfigImpl.class);
        public final String[] EXTERNALIZED_PROPERTY_DIR = System.getProperty(LIGHT_4J_CONFIG_DIR, "").split(File.pathSeparator);
        private volatile ConfigLoader configLoader;
        private final Object configLoaderLock = new Object();
        private volatile Thread configLoaderInitializingThread;
        private volatile ClassLoader classLoader;
        private final String configLoaderClass;
        // Shared across Config instances: a callback must never wait on another cache-entry lock.
        private static final ThreadLocal<Boolean> activeCacheLoad = new ThreadLocal<>();

        // An entry is also the invalidation token for a name. Removing/replacing it detaches
        // in-flight loads: they may finish for their caller but cannot repopulate the cache.
        private final Map<String, CacheEntry> configCache = new ConcurrentHashMap<>(10, 0.9f, 1);

        private static final class CacheEntry {
            private final Object loadLock = new Object();
            final Map<Class<?>, Object> representations = new ConcurrentHashMap<>();
            final Object injected;
            int readers; // Guarded by configCache; tracks users of an initially empty entry.

            CacheEntry() { this(null); }

            CacheEntry(Object injected) {
                this.injected = injected;
                if (injected != null) {
                    representations.put(injected instanceof Map ? Map.class : injected.getClass(), injected);
                }
            }
        }

        private <T> T withCacheEntry(String name, java.util.function.Function<CacheEntry, T> action) {
            // Constructors and load callbacks may read other names. Reuse completed values,
            // but load nested misses in a detached entry: no second shared entry lock, and
            // no publication of bootstrap data or partially resolved nested representations.
            if (configLoaderInitializingThread == Thread.currentThread()
                    || Boolean.TRUE.equals(activeCacheLoad.get())) {
                CacheEntry existing = configCache.get(name);
                CacheEntry bootstrap = new CacheEntry(existing == null ? null : existing.injected);
                if (existing != null) bootstrap.representations.putAll(existing.representations);
                return action.apply(bootstrap);
            }
            CacheEntry entry = configCache.get(name);
            if (entry != null && !entry.representations.isEmpty()) return action.apply(entry);
            synchronized (configCache) {
                entry = configCache.computeIfAbsent(name, ignored -> new CacheEntry());
                entry.readers++;
            }
            try {
                return action.apply(entry);
            } finally {
                synchronized (configCache) {
                    if (--entry.readers == 0 && entry.representations.isEmpty()) {
                        configCache.remove(name, entry);
                    }
                }
            }
        }

        private void initializeConfigLoader() {
            if (configLoaderClass == null || configLoader != null
                    || configLoaderInitializingThread == Thread.currentThread()) return;
            synchronized (configLoaderLock) {
                if (configLoader == null) {
                    configLoaderInitializingThread = Thread.currentThread();
                    try {
                        configLoader = ConfigLoaderConstructor.getInstance(configLoaderClass).getConfigLoader();
                    } finally {
                        configLoaderInitializingThread = null;
                    }
                }
            }
        }

        private Object cached(CacheEntry entry, Class<?> type, java.util.function.Supplier<Object> loader) {
            Object existing = entry.representations.get(type);
            if (existing != null) return existing;
            synchronized (entry.loadLock) {
                Object value = entry.representations.get(type);
                if (value == null && type.isInstance(entry.injected)) {
                    value = entry.injected;
                }
                if (value == null) {
                    Boolean previousLoad = activeCacheLoad.get();
                    activeCacheLoad.set(true);
                    try {
                        value = loader.get();
                    } finally {
                        if (previousLoad == null) activeCacheLoad.remove();
                        else activeCacheLoad.set(previousLoad);
                    }
                    if (value != null && !type.isInstance(value)) {
                        throw new ConfigException("Config loader returned " + value.getClass().getName()
                                + " instead of " + type.getName());
                    }
                }
                if (value != null) entry.representations.put(type, value);
                return value;
            }
        }

        private Map<String, Object> cachedMap(CacheEntry entry, String name, String path, boolean defaults) {
            return (Map<String, Object>) cached(entry, Map.class, () -> defaults
                    ? loadMapConfig(name, path) : loadJsonMapConfigWithSpecificConfigLoader(name, path));
        }

        private Object cachedValue(String name, Class<?> type) {
            CacheEntry entry = configCache.get(name);
            if (entry == null) return null;
            Object value = entry.representations.get(type);
            if (value != null) return value;
            return type.isInstance(entry.injected) ? entry.injected : null;
        }

        private Object cachedObject(String name, Class<?> type, String path, boolean defaults) {
            if (!defaults) {
                Object existing = cachedValue(name, type);
                if (existing != null) return existing;
                initializeConfigLoader();
            }
            return withCacheEntry(name, entry -> {
                // Share map identity, but preserve a custom loader's object dispatch when
                // the object API is the first reader of this representation.
                if (type == Map.class && (defaults || configLoaderClass == null)) {
                    return cachedMap(entry, name, path, defaults);
                }
                return cached(entry, type, () -> {
                    // A configured loader may implement object binding differently.
                    // Exclusions use SnakeYAML binding rather than Jackson.
                    if (configLoaderClass == null && !ConfigInjection.isExclusionConfigFile(name)) {
                        Map<String, Object> map = cachedMap(entry, name, path, defaults);
                        try {
                            return map == null ? null : CentralizedManagement.convertMapToObj(map, type);
                        } catch (IllegalArgumentException e) {
                            logger.error("Unable to bind configuration {} at {} as {}", name, path, type.getName(), e);
                            throw new RuntimeException("Unable to load configuration '" + name + "' as object.", e);
                        }
                    }
                    return defaults ? loadObjectConfig(name, type, path)
                            : loadJsonObjectConfigWithSpecificConfigLoader(name, type, path);
                });
            });
        }

        // An instance of Jackson ObjectMapper that can be used anywhere else for Json.
        final static ObjectMapper mapper = new ObjectMapper();

        static {
            mapper.registerModule(new JavaTimeModule());
            mapper.registerModule(new Jdk8Module());
            mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        }

        public AbstractConfigImpl() {
            configLoaderClass = getConfigLoaderClass();
        }

        @Override
        public ObjectMapper getMapper() {
            return mapper;
        }
        @Override
        public void clear() {
            synchronized (configCache) {
                configCache.clear();
            }
        }

        @Override
        public void clearConfigCache(String configName) {
            CacheEntry object;
            synchronized (configCache) {
                object = configCache.remove(configName);
            }
            if(object != null && !object.representations.isEmpty() && logger.isInfoEnabled()) {
                logger.info("Config file {} is removed from the cache for reloading.", configName);
            }
        }

        @Override
        public void setClassLoader(ClassLoader classLoader) {
            synchronized (configCache) {
                this.classLoader = classLoader;
                Map<String, CacheEntry> injections = new HashMap<>();
                configCache.forEach((name, entry) -> {
                    if (entry.injected != null) injections.put(name, new CacheEntry(entry.injected));
                });
                configCache.clear();
                configCache.putAll(injections);
            }
        }

        private ClassLoader getClassLoader() {
            if (this.classLoader != null) {
                return this.classLoader;
            }
            return getClass().getClassLoader();
        }

        public void putInConfigCache(String configName, Object config) {
            Objects.requireNonNull(config);
            CacheEntry entry = new CacheEntry(config);
            synchronized (configCache) {
                configCache.put(configName, entry);
            }
        }

        @Override
        public String getStringFromFile(String filename, String path) {
            return withCacheEntry(filename, entry -> (String) cached(entry, String.class, () -> loadStringFromFile(filename, path)));
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
         * Each representation retains its identity until the name is invalidated or replaced.
         * Configured/default accessors share entries; the first load of a representation wins.
         * Without a configured loader, non-exclusion objects derive from the cached map. Later map mutations
         * do not update an already cached object.
         * @param configName    The name of the config file, without an extension
         * @param clazz         The class that the object will be deserialized into
         * @param path          The relative directory or absolute directory that config will be loaded from
         * @return An instance of the object if possible, null otherwise. IOExceptions smothered.
         */
        @Override
        public Object getJsonObjectConfig(String configName, Class clazz, String path) {
            return cachedObject(configName, clazz, path, false);
        }

        /**
         * Method used to load the configuration file as a given Object by using default loading method and cache it.
         * Each representation retains its identity until the name is invalidated or replaced.
         * Configured/default accessors share entries; the first load of a representation wins.
         * Without a configured loader, non-exclusion objects derive from the cached map. Later map mutations
         * do not update an already cached object.
         * @param configName    The name of the config file, without an extension
         * @param clazz         The class that the object will be deserialized into
         * @param path          The relative directory or absolute directory that config will be loaded from
         * @return An instance of the object if possible, null otherwise. IOExceptions smothered.
         */
        @Override
        public Object getDefaultJsonObjectConfig(String configName, Class clazz, String path) {
            return cachedObject(configName, clazz, path, true);
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
         * Method used to load the configuration file as a given Object based on the config loader class configured in config.yml without cache.
         * @param configName    The name of the config file, without an extension
         * @param clazz         The class that the object will be deserialized into
         * @return An instance of the object if possible, null otherwise. IOExceptions smothered.
         */
        @Override
        public Object getJsonObjectConfigNoCache(String configName, Class clazz) {
            return loadJsonObjectConfigWithSpecificConfigLoader(configName, clazz, "");
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
         * Each representation retains its identity until the name is invalidated or replaced.
         * Configured/default accessors share entries; the first load of a representation wins.
         * Without a configured loader, non-exclusion objects derive from the cached map. Later map mutations
         * do not update an already cached object.
         * @param configName    The name of the config file, without an extension
         * @param path          The relative directory or absolute directory that config will be loaded from
         * @return A map of the config fields if possible, null otherwise. IOExceptions smothered.
         */
        @Override
        public Map<String, Object> getJsonMapConfig(String configName, String path) {
            Object existing = cachedValue(configName, Map.class);
            if (existing != null) return (Map<String, Object>) existing;
            initializeConfigLoader();
            return withCacheEntry(configName, entry -> cachedMap(entry, configName, path, false));
        }

        /**
         * Method used to load the configuration file as a map by using default loading method and cache it.
         * Each representation retains its identity until the name is invalidated or replaced.
         * Configured/default accessors share entries; the first load of a representation wins.
         * Without a configured loader, non-exclusion objects derive from the cached map. Later map mutations
         * do not update an already cached object.
         * @param configName    The name of the config file, without an extension
         * @param path          The relative directory or absolute directory that config will be loaded from
         * @return A map of the config fields if possible, null otherwise. IOExceptions smothered.
         */
        @Override
        public Map<String, Object> getDefaultJsonMapConfig(String configName, String path) {
            return withCacheEntry(configName, entry -> cachedMap(entry, configName, path, true));
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
                        synchronized (getYaml()) {
                            config = getYaml().loadAs(inStream, clazz);
                        }
                    } else {
                        // Parse into map first, since map is easier to be manipulated in merging process
                        Map<String, Object> configMap;
                        synchronized (getYaml()) {
                            configMap = getYaml().load(inStream);
                        }
                        config = CentralizedManagement.mergeObject(isDecrypt(), configMap, clazz);
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
                if (inStream != null && inStream.available() > 0) {
                    synchronized (getYaml()) {
                        config = getYaml().load(inStream);
                    }
                    if (!ConfigInjection.isExclusionConfigFile(configName)) {
                        CentralizedManagement.mergeMap(isDecrypt(), config); // mutates the config map in place.
                    }
                }
            } catch (Exception e) {
                logger.error("Exception on loading {}", ymlFilename, e);
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
                    if (logger.isTraceEnabled() && configFilename != null && !configFilename.contains(CENTRALIZED_MANAGEMENT)) {
                        logger.trace("Unable to load config from externalized folder for {}", Encode.forJava(configFilename + " in " + absolutePath));
                    }
                }
                // absolute path do not need to continue
                if (path.startsWith("/")) break;
            }
            if (inStream != null) {
                if (logger.isTraceEnabled() && configFilename != null && !configFilename.contains(CENTRALIZED_MANAGEMENT)) {
                    logger.trace("Config loaded from externalized folder for {}", Encode.forJava(configFilename + " in " + configFileDir));
                }
                return inStream;
            }
            if (logger.isTraceEnabled() && configFilename != null && !configFilename.contains(CENTRALIZED_MANAGEMENT)) {
                logger.trace("Trying to load config from classpath directory for file {}", Encode.forJava(configFilename));
            }
            inStream = this.getClassLoader().getResourceAsStream(configFilename);
            if (inStream != null) {
                if (logger.isTraceEnabled() && configFilename != null && !configFilename.contains(CENTRALIZED_MANAGEMENT)) {
                    logger.trace("config loaded from classpath for {}", Encode.forJava(configFilename));
                }
                return inStream;
            }
            inStream = this.getClassLoader().getResourceAsStream("config/" + configFilename);
            if (inStream != null) {
                if (logger.isTraceEnabled() && configFilename != null && !configFilename.contains(CENTRALIZED_MANAGEMENT)) {
                    logger.trace("Config loaded from default folder for {}", Encode.forJava(configFilename));
                }
                return inStream;
            }
            assert configFilename != null;
            if (configFilename.endsWith(CONFIG_EXT_YML)) {
                logger.trace("Unable to load config {}. Looking for the same file name with extension yaml...", Encode.forJava(configFilename));
            } else if (configFilename.endsWith(CONFIG_EXT_YAML)) {
                logger.trace("Unable to load config {}. Looking for the same file name with extension json...", Encode.forJava(configFilename));
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
                return path.isEmpty() ? EXTERNALIZED_PROPERTY_DIR[index].trim() : EXTERNALIZED_PROPERTY_DIR[index].trim() + "/" + path;
            }
        }

        protected String getDecryptorClass() {
            Map<String, Object> config = loadModuleConfig();
            if (null != config) {
                String decryptorClass = (String) config.get(DecryptConstructor.CONFIG_ITEM_DECRYPTOR_CLASS);
                if (logger.isTraceEnabled()) {
                    logger.trace("found decryptorClass={}", decryptorClass);
                }
                return decryptorClass == null ? DecryptConstructor.DEFAULT_DECRYPTOR_CLASS : decryptorClass;
            }else {
                logger.warn("config file cannot be found.");
            }
            return DecryptConstructor.DEFAULT_DECRYPTOR_CLASS;
        }

        // public String getDecryptorClassPublic() { return getDecryptorClass(); }

        private String getConfigLoaderClass() {
            Map<String, Object> config = loadModuleConfig();
            if (null != config) {
                String configLoaderClass = (String) config.get(ConfigLoaderConstructor.CONFIG_LOADER_CLASS);
                if (logger.isTraceEnabled()) {
                    logger.trace("found configLoaderClass={}", configLoaderClass);
                }
                return configLoaderClass;
            }else {
                logger.warn("config file cannot be found.");
            }
            return null;
        }

        private Map<String, Object> loadModuleConfigNoCache() {
            Yaml yml = new Yaml();  // The caller is synced, so it is thread safe here.

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
                    if (logger.isTraceEnabled()) {
                        logger.trace("loaded config from file {}", ymlFilename);
                    }
                    break;
                }
            }
            return config;
        }

        private Map<String, Object> loadModuleConfig() {
            return withCacheEntry(CONFIG_NAME, entry -> (Map<String, Object>) cached(entry, Map.class, this::loadModuleConfigNoCache));
        }

        private Map<String, Object> loadJsonMapConfigWithSpecificConfigLoader(String configName, String path) {
            Map<String, Object> config = null;
            initializeConfigLoader();
            if (configLoader != null) {
                logger.trace("Trying to load {} with extension yaml, yml or json by using ConfigLoader: {}.", configName, configLoader.getClass().getName());
                if (path == null || path.equals("")) {
                    config = configLoader.loadMapConfig(configName);
                } else {
                    config = configLoader.loadMapConfig(configName, path);
                }
            }
            // Fall back to default loading method if the configuration cannot be loaded by specific config loader
            if (config == null) {
                logger.trace("Trying to load {} with extension yaml, yml or json by using default loading method.", configName);
                config = loadMapConfig(configName, path);
            }
            return config;
        }

        private Object loadJsonObjectConfigWithSpecificConfigLoader(String configName, Class clazz, String path) {
            Object config = null;
            initializeConfigLoader();
            if (this.configLoader != null) {
                logger.trace("Trying to load {} with extension yaml, yml or json by using ConfigLoader: {}.", configName, configLoader.getClass().getName());
                if (path == null || path.isEmpty()) {
                    config = configLoader.loadObjectConfig(configName, clazz);
                } else {
                    config = configLoader.loadObjectConfig(configName, clazz, path);
                }
            }
            // Fall back to default loading method if the configuration cannot be loaded by specific config loader
            if (config == null) {
                logger.trace("Trying to load {} with extension yaml, yml or json by using default loading method.", configName);
                config = loadObjectConfig(configName, clazz, path);
            }
            return config;
        }

    }
    /**
     * This is the implementation that is not decrypt the CRYPT value from the config file. So that it
     * can be used to load the configuration for module registry and output with the server info endpoint.
     * @author Steve Hu
     */
    private static final class NoneDecryptedConfigImpl extends AbstractConfigImpl {
        private static final Config NONE_DECRYPTED = initialize();
        final Yaml yaml;
        NoneDecryptedConfigImpl() {
            super();
            synchronized (NoneDecryptedConfigImpl.class) {
                yaml = new Yaml();
            }
        }

        private static Config initialize() {
            Iterator<Config> it;
            it = ServiceLoader.load(Config.class).iterator();
            return it.hasNext() ? it.next() : new NoneDecryptedConfigImpl();
        }

        @Override
        public Yaml getYaml() {
            return yaml;
        }

        @Override
        public boolean isDecrypt() {
            return false;
        }
    }

    private static final class FileConfigImpl extends AbstractConfigImpl {
        private static final Config DEFAULT = initialize();


        final Yaml yaml;

        FileConfigImpl(){
        	super();
      	    String decryptorClass = getDecryptorClass();
            synchronized (FileConfigImpl.class) {
                if (null == decryptorClass || decryptorClass.trim().isEmpty()) {
                    yaml = new Yaml();
                } else {
                    final Resolver resolver = new Resolver();
                    resolver.addImplicitResolver(YmlConstants.CRYPT_TAG, YmlConstants.CRYPT_PATTERN, YmlConstants.CRYPT_FIRST);
                    yaml = new Yaml(DecryptConstructor.getInstance(decryptorClass), new Representer(new DumperOptions()), new DumperOptions(), resolver);
                }
            }
        }

        private static Config initialize() {
            Iterator<Config> it;
            it = ServiceLoader.load(Config.class).iterator();
            return it.hasNext() ? it.next() : new FileConfigImpl();
        }

        @Override
        public Yaml getYaml() {
            return yaml;
        }

        @Override
        public boolean isDecrypt() {
            return true;
        }
    }

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static Boolean loadBooleanValue(String name, Object object) {
        if (object instanceof Boolean) {
            return (Boolean) object;
        } else if (object instanceof String) {
            String s = (String)object;
            s = s.trim();
            if(s.isEmpty()) return false;
            return Boolean.valueOf(s);
        } else {
            throw new ConfigException(name + " must be a boolean or a string value.");
        }
    }

    public static Integer loadIntegerValue(String name, Object object) {
        if (object instanceof Integer) {
            return (Integer) object;
        } else if (object instanceof String) {
            String s = (String)object;
            s = s.trim();
            if(s.isEmpty()) return 0;
            return Integer.valueOf(s);
        } else {
            throw new ConfigException(name + " must be an integer or a string value.");
        }
    }

    public static Long loadLongValue(String name, Object object) {
        if (object instanceof Integer) {
            return Long.valueOf((Integer) object);
        } else if(object instanceof Long) {
            return (Long) object;
        } else if (object instanceof String) {
            String s = (String)object;
            s = s.trim();
            if(s.isEmpty()) return 0L;
            return Long.valueOf(s);
        } else {
            throw new ConfigException(name + " must be a long or a string value.");
        }
    }

    /**
     * Loads a double value from a configuration object, handling conversion from Double, String, Integer, or Long.
     * @param name The name of the configuration property (used for error messages).
     * @param object The raw object value retrieved from the configuration map.
     * @return The loaded Double value.
     * @throws ConfigException if the object is not a valid double representation.
     */
    public static Double loadDoubleValue(String name, Object object) {
        if (object instanceof Double) {
            return (Double) object;
        } else if (object instanceof Float) {
            return Double.valueOf((Float) object);
        } else if (object instanceof Integer) {
            return Double.valueOf((Integer) object);
        } else if (object instanceof Long) {
            return Double.valueOf((Long) object);
        } else if (object instanceof String) {
            String s = (String)object;
            s = s.trim();
            if(s.isEmpty()) return 0.0;
            try {
                return Double.valueOf(s);
            } catch (NumberFormatException e) {
                throw new ConfigException(name + " must be a double or a string value that can be parsed to a double.");
            }
        } else {
            throw new ConfigException(name + " must be a double, integer, long, or a string value.");
        }
    }

    static InputStream convertStringToStream(String string) {
        return new ByteArrayInputStream(string.getBytes(UTF_8));
    }
}
