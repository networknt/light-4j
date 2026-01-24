package com.networknt.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.ConfigException;
import com.networknt.config.schema.ArrayField;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.OutputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.networknt.config.Config;

@ConfigSchema(configName = "cache", configKey = "cache", outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD})
public class CacheConfig {
    private static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);
    public static final String CONFIG_NAME = "cache";
    public static final String CACHES = "caches";
    public static final String CACHE_NAME = "cacheName";
    public static final String EXPIRY_IN_MINUTES = "expiryInMinutes";
    public static final String MAX_SIZE = "maxSize";

    @ArrayField(
            configFieldName = CACHES,
            externalizedKeyName = CACHES,
            description = "There will be multiple caches per application and each cache should have it own name and expiryInMinutes. The\n" +
            "caches are lists of caches. The cache name is used to identify the cache and the expiryInMinutes the expiry time.\n" +
            "caches:\n" +
            "  - cacheName: cache1\n" +
            "    expiryInMinutes: 60\n" +
            "    maxSize: 1000\n" +
            "  - cacheName: cache2\n" +
            "    expiryInMinutes: 120\n" +
            "    maxSize: 100",
            items = CacheItem.class
    )
    List<CacheItem> caches;

    private final Config config;
    private Map<String, Object> mappedConfig;

    private CacheConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfig(configName);
        setConfigList();
    }

    private CacheConfig() {
        this(CONFIG_NAME);
    }

    public static CacheConfig load() {
        return new CacheConfig();
    }

    public static CacheConfig load(String configName) {
        return new CacheConfig(configName);
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    public List<CacheItem> getCaches() {
        return caches;
    }

    public void setCaches(List<CacheItem> caches) {
        this.caches = caches;
    }

    public void setConfigList() {
        if (mappedConfig != null && mappedConfig.get(CACHES) != null) {
            Object object = mappedConfig.get(CACHES);
            caches = new ArrayList<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("caches s = " + s);
                if(s.startsWith("[")) {
                    // json format
                    try {
                        caches = Config.getInstance().getMapper().readValue(s, new TypeReference<List<CacheItem>>() {});
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the caches json with a list of string and object.");
                    }
                } else {
                    throw new ConfigException("caches must be a list of string object map.");
                }
            } else if (object instanceof List) {
                // the object is a list of map, we need convert it to CacheItem object.
                List<Map<String, Object>> values = (List<Map<String, Object>>)object;
                for(Map<String, Object> value: values) {
                    CacheItem cacheItem = new CacheItem();
                    cacheItem.setCacheName((String)value.get(CACHE_NAME));
                    cacheItem.setMaxSize((Integer)value.get(MAX_SIZE));
                    cacheItem.setExpiryInMinutes((Integer)value.get(EXPIRY_IN_MINUTES));
                    caches.add(cacheItem);
                }
            } else {
                throw new ConfigException("caches must be a list of string object map.");
            }

        }
    }
}
