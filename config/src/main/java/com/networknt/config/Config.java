package com.networknt.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.owasp.encoder.Encode;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A injectable singleton config that has default implementation
 * based on FileSystem json files. It can be extended to
 * other sources (database, distributed cache etc.) by providing
 * another jar in the classpath to replace the default implementation.
 *
 * Config files are loaded in the following sequence:
 * 1. resources/config folder for the default
 * 2. externalized directory specified by undertow-server-config-dir
 *
 * In docker, the config files should be in volume and any update will
 * be picked up the next day morning.
 *
 *
 */
public abstract class Config {
    protected Config() {
    }

    // abstract methods that need be implemented by all implementations
    public abstract Map<String, Object> getJsonMapConfig(String configName);

    public abstract Map<String, Object> getJsonMapConfigNoCache(String configName);

    public abstract JsonNode getJsonNodeConfig(String configName);

    public abstract Object getJsonObjectConfig(String configName, Class clazz);

    public abstract String getStringFromFile(String filename);

    public abstract InputStream getInputStreamFromFile(String filename);

    public abstract ObjectMapper getMapper();

    public abstract void clear();

    public static Config getInstance() {
        return FileConfigImpl.DEFAULT;
    }

    private static final class FileConfigImpl extends Config {
        static final String CONFIG_EXT_JSON = ".json";

        static final XLogger logger = XLoggerFactory.getXLogger(Config.class);

        static final String EXTERNALIZED_PROPERTY_DIR = System.getProperty("undertow-server-config-dir", "");

        private long cacheExpirationTime = 0L;

        private static final Config DEFAULT = initialize();

        // Memory cache of all the configuration object. Each config will be loaded on the first time is is accessed.
        Map<String, Object> configCache = new ConcurrentHashMap<String, Object>(10, 0.9f, 1);

        // An instance of Jackson ObjectMapper that can be used anywhere else for Json.
        ObjectMapper mapper = new ObjectMapper();

        private static Config initialize() {
            Iterator<Config> it = null;
            it = ServiceLoader.load(Config.class).iterator();
            return it != null && it.hasNext() ? it.next() : new FileConfigImpl();
        }

        // Return instance of Jackson Object Mapper
        @Override
        public ObjectMapper getMapper() {
            return mapper;
        }

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
            Object config = (Object)configCache.get(configName);
            if(config == null) {
                synchronized (FileConfigImpl.class) {
                    config = (Object)configCache.get(configName);
                    if(config == null) {
                        config = loadJsonObjectConfig(configName, clazz);
                        if(config != null) configCache.put(configName, config);
                    }
                }
            }
            return config;
        }

        @Override
        public JsonNode getJsonNodeConfig(String configName) {
            checkCacheExpiration();
            JsonNode config = (JsonNode)configCache.get(configName);
            if(config == null) {
                synchronized (FileConfigImpl.class) {
                    config = (JsonNode)configCache.get(configName);
                    if(config == null) {
                        config = loadJsonNodeConfig(configName);
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
                        config = loadJsonMapConfig(configName);
                        if(config != null) configCache.put(configName, config);
                    }
                }
            }
            return config;
        }

        @Override
        public Map<String, Object> getJsonMapConfigNoCache(String configName) {
            return loadJsonMapConfig(configName);
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
                logger.catching(ioe);
            } finally {
                if(inStream != null) {
                    try {
                        inStream.close();
                    } catch(IOException ioe) {
                        logger.catching(ioe);
                    }
                }
            }
            return content;
        }

        private Object loadJsonObjectConfig(String configName, Class clazz) {
            Object config = null;
            String configFilename = configName + CONFIG_EXT_JSON;
            InputStream inStream = null;
            try {
                inStream = getConfigStream(configFilename);
                if(inStream != null) {
                    config = mapper.readValue(inStream, clazz);
                }
            } catch (IOException ioe) {
                logger.catching(ioe);
            } finally {
                if(inStream != null) {
                    try {
                        inStream.close();
                    } catch(IOException ioe) {
                        logger.catching(ioe);
                    }
                }
            }
            return config;
        }


        private JsonNode loadJsonNodeConfig(String configName) {
            JsonNode config = null;
            String configFilename = configName + CONFIG_EXT_JSON;
            InputStream inStream = null;
            try {
                inStream = getConfigStream(configFilename);
                if(inStream != null) {
                    config = mapper.readValue(inStream, JsonNode.class);
                }
            } catch (IOException ioe) {
                logger.catching(ioe);
            } finally {
                if(inStream != null) {
                    try {
                        inStream.close();
                    } catch(IOException ioe) {
                        logger.catching(ioe);
                    }
                }
            }
            return config;
        }

        private Map<String, Object> loadJsonMapConfig(String configName) {
            Map<String, Object> config = null;
            String configFilename = configName + CONFIG_EXT_JSON;
            InputStream inStream = null;
            try {
                inStream = getConfigStream(configFilename);
                if(inStream != null) {
                    config = mapper.readValue(inStream, new TypeReference<HashMap<String, Object>>() {});
                }
            } catch (IOException ioe) {
                logger.catching(ioe);
            } finally {
                if(inStream != null) {
                    try {
                        inStream.close();
                    } catch(IOException ioe) {
                        logger.catching(ioe);
                    }
                }
            }
            return config;
        }

        private InputStream getConfigStream(String configFilename) {

            InputStream inStream = null;
            try{
            	inStream = new FileInputStream(EXTERNALIZED_PROPERTY_DIR + "/" + configFilename);
            } catch (FileNotFoundException ex){
                logger.info("Unable to load config from externalized folder for " + Encode.forJava(configFilename + " in " + EXTERNALIZED_PROPERTY_DIR));
            }
            if(inStream != null) {
                logger.info("Config loaded from externalized folder for " + Encode.forJava(configFilename + " in " + EXTERNALIZED_PROPERTY_DIR));
                return inStream;
            }
            inStream = getClass().getClassLoader().getResourceAsStream("config/" + configFilename);
            if(inStream != null) {
                logger.info("Config loaded from default folder for " + Encode.forJava(configFilename));
                return inStream;
            }
            logger.error("Unable to load config " + Encode.forJava(configFilename));
            return inStream;
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
