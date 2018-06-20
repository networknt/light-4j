/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A injectable singleton config that has default implementation
 * based on FileSystem json files. It can be extended to
 * other sources (database, distributed cache etc.) by providing
 * another jar in the classpath to replace the default implementation.
 *
 * Config files are loaded in the following sequence:
 * 1. resource/config folder for the default
 * 2. externalized directory specified by light-4j-config-dir
 *
 * In docker, the config files should be in volume and any update will
 * be picked up the next day morning.
 *
 *
 */
public abstract class Config {
    public static final String LIGHT_4J_CONFIG_DIR = "light-4j-config-dir";

    protected Config() {
    }

    // abstract methods that need be implemented by all implementations
    public abstract Map<String, Object> getJsonMapConfig(String configName);

    public abstract Map<String, Object> getJsonMapConfigNoCache(String configName);

    //public abstract JsonNode getJsonNodeConfig(String configName);

    public abstract Object getJsonObjectConfig(String configName, Class clazz);

    public abstract String getStringFromFile(String filename);

    public abstract InputStream getInputStreamFromFile(String filename);

    public abstract ObjectMapper getMapper();

    public abstract Yaml getYaml();

    public abstract void clear();

    public static Config getInstance() {
        return FileConfigImpl.DEFAULT;
    }

    private static final class FileConfigImpl extends Config {
        static final String CONFIG_EXT_JSON = ".json";
        static final String CONFIG_EXT_YAML = ".yaml";
        static final String CONFIG_EXT_YML = ".yml";

        static final Logger logger = LoggerFactory.getLogger(Config.class);

        public final String EXTERNALIZED_PROPERTY_DIR = System.getProperty(LIGHT_4J_CONFIG_DIR, "");

        private long cacheExpirationTime = 0L;

        private static final Config DEFAULT = initialize();

        // Memory cache of all the configuration object. Each config will be loaded on the first time is is accessed.
        final Map<String, Object> configCache = new ConcurrentHashMap<>(10, 0.9f, 1);

        // An instance of Jackson ObjectMapper that can be used anywhere else for Json.
        final static ObjectMapper mapper = new ObjectMapper();
        static {
            mapper.registerModule(new JavaTimeModule());
            mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        }

        final Yaml yaml = new Yaml();

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
        public String getStringFromFile(String filename) {
            checkCacheExpiration();
            String content = (String)configCache.get(filename);
            if(content == null) {
                synchronized (FileConfigImpl.class) {
                    content = (String)configCache.get(filename);
                    if(content == null) {
                        content = loadStringFromFile(filename);
                        if(content != null) configCache.put(filename, content);
                    }
                }
            }
            return content;
        }

        @Override
        public InputStream getInputStreamFromFile(String filename) {
            return getConfigStream(filename);
        }

        @Override
        public Object getJsonObjectConfig(String configName, Class clazz) {
            checkCacheExpiration();
            Object config = configCache.get(configName);
            if(config == null) {
                synchronized (FileConfigImpl.class) {
                    config = configCache.get(configName);
                    if(config == null) {
                        config = loadObjectConfig(configName, clazz);
                        if(config != null) configCache.put(configName, config);
                    }
                }
            }
            return config;
        }

        @Override
        public Map<String, Object> getJsonMapConfig(String configName) {
            checkCacheExpiration();
            Map<String, Object> config = (Map<String, Object>)configCache.get(configName);
            if(config == null) {
                synchronized (FileConfigImpl.class) {
                    config = (Map<String, Object>)configCache.get(configName);
                    if(config == null) {
                        config = loadMapConfig(configName);
                        if(config != null) configCache.put(configName, config);
                    }
                }
            }
            return config;
        }

        @Override
        public Map<String, Object> getJsonMapConfigNoCache(String configName) {
            return loadMapConfig(configName);
        }

        private String loadStringFromFile(String filename) {
            String content = null;
            InputStream inStream = null;
            try {
                inStream = getConfigStream(filename);
                if(inStream != null) {
                    content = convertStreamToString(inStream);
                }
            } catch (Exception ioe) {
                logger.error("Exception", ioe);
            } finally {
                if(inStream != null) {
                    try {
                        inStream.close();
                    } catch(IOException ioe) {
                        logger.error("IOException", ioe);
                    }
                }
            }
            return content;
        }

        private Object loadObjectConfig(String configName, Class clazz) {
            Object config = null;

            String ymlFilename = configName + CONFIG_EXT_YML;
            try (InputStream inStream = getConfigStream(ymlFilename)) {
                if(inStream != null) {
                    config = yaml.loadAs(inStream, clazz);
                }
            } catch (IOException ioe) {
                logger.error("IOException", ioe);
            }
            if(config != null) return config;

            String yamlFilename = configName + CONFIG_EXT_YAML;
            try (InputStream inStream = getConfigStream(yamlFilename)) {
                if(inStream != null) {
                    config = yaml.loadAs(inStream, clazz);
                }
            } catch (IOException ioe) {
                logger.error("IOException", ioe);
            }
            if(config != null) return config;

            String jsonFilename = configName + CONFIG_EXT_JSON;
            try (InputStream inStream = getConfigStream(jsonFilename)) {
                if(inStream != null) {
                    config = mapper.readValue(inStream, clazz);
                }
            } catch (IOException ioe) {
                logger.error("IOException", ioe);
            }
            return config;
        }

        private Map<String, Object> loadMapConfig(String configName) {
            Map<String, Object> config = null;

            String ymlFilename = configName + CONFIG_EXT_YML;
            try (InputStream inStream = getConfigStream(ymlFilename)) {
                if(inStream != null) {
                    config = (Map<String, Object>)yaml.load(inStream);
                }
            } catch (IOException ioe) {
                logger.error("IOException", ioe);
            }
            if(config != null) return config;

            String yamlFilename = configName + CONFIG_EXT_YAML;
            try (InputStream inStream = getConfigStream(yamlFilename)) {
                if(inStream != null) {
                    config = (Map<String, Object>)yaml.load(inStream);
                }
            } catch (IOException ioe) {
                logger.error("IOException", ioe);
            }
            if(config != null) return config;

            String configFilename = configName + CONFIG_EXT_JSON;
            try (InputStream inStream = getConfigStream(configFilename)){
                if(inStream != null) {
                    config = mapper.readValue(inStream, new TypeReference<HashMap<String, Object>>() {});
                }
            } catch (IOException ioe) {
                logger.error("IOException", ioe);
            }
            return config;
        }

        private InputStream getConfigStream(String configFilename) {

            InputStream inStream = null;
            try{
            	inStream = new FileInputStream(EXTERNALIZED_PROPERTY_DIR + "/" + configFilename);
            } catch (FileNotFoundException ex){
                if(logger.isInfoEnabled()) {
                    logger.info("Unable to load config from externalized folder for " + Encode.forJava(configFilename + " in " + EXTERNALIZED_PROPERTY_DIR));
                }
            }
            if(inStream != null) {
                if(logger.isInfoEnabled()) {
                    logger.info("Config loaded from externalized folder for " + Encode.forJava(configFilename + " in " + EXTERNALIZED_PROPERTY_DIR));
                }
                return inStream;
            }
            if(logger.isInfoEnabled()) {
                logger.info("Trying to load config from classpath directory for file " + Encode.forJava(configFilename));
            }
            inStream = getClass().getClassLoader().getResourceAsStream(configFilename);
            if(inStream != null) {
                if(logger.isInfoEnabled()) {
                    logger.info("config loaded from classpath for " + Encode.forJava(configFilename));
                }
                return inStream;
            }
            inStream = getClass().getClassLoader().getResourceAsStream("config/" + configFilename);
            if(inStream != null) {
                if(logger.isInfoEnabled()) {
                    logger.info("Config loaded from default folder for " + Encode.forJava(configFilename));
                }
                return inStream;
            }
            logger.error("*****Unable to load config " + Encode.forJava(configFilename));
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

        private void checkCacheExpiration() {
            if(System.currentTimeMillis() > cacheExpirationTime) {
                clear();
                logger.info("daily config cache refresh");
                cacheExpirationTime = getNextMidNightTime();
            }
        }

    }

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
