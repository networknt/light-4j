package com.networknt.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.schema.*;
import com.networknt.server.ModuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ConfigSchema(configKey = "request-injection", configName = "request-injection", outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD})
public class RequestInjectionConfig {
    private static final Logger LOG = LoggerFactory.getLogger(RequestInjectionConfig.class);
    public static final String CONFIG_NAME = "request-injection";
    private static final String ENABLED = "enabled";
    private static final String APPLIED_BODY_INJECTION_PATH_PREFIXES = "appliedBodyInjectionPathPrefixes";
    private static final String MAX_BUFFERS = "maxBuffers";

    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            defaultValue = "true",
            description = "indicator of enabled"
    )
    private boolean enabled;

    @ArrayField(
            configFieldName = APPLIED_BODY_INJECTION_PATH_PREFIXES,
            externalizedKeyName = APPLIED_BODY_INJECTION_PATH_PREFIXES,
            description = "request body injection applied path prefixes. Injecting the request body and output into the audit log is very heavy operation,\n" +
                    "and it should only be enabled when necessary or for diagnose session to resolve issues. This list can be updated on the config\n" +
                    "server or local values.yml, then an API call to the config-reload endpoint to apply the changes from light-portal control pane.\n" +
                    "Please be aware that big request body will only log the beginning part of it in the audit log and gzip encoded request body can\n" +
                    "not be injected. Even the body injection is not applied, you can still transform the request for headers, query parameters, path\n" +
                    "parameters etc. The format is a list of strings separated with commas or a JSON list in values.yml definition from config server,\n" +
                    "or you can use yaml format in this file or values.yaml on local filesystem. The following are the examples.\n" +
                    "request-injection.appliedBodyInjectionPathPrefixes: [\"/v1/cats\", \"/v1/dogs\"]\n" +
                    "request-injection.appliedBodyInjectionPathPrefixes: /v1/cats, /v1/dogs\n" +
                    "request-injection.appliedBodyInjectionPathPrefixes:\n" +
                    "  - /v1/cats\n" +
                    "  - /v1/dogs",
            items = String.class
    )
    private List<String> appliedBodyInjectionPathPrefixes;

    @IntegerField(
            configFieldName = MAX_BUFFERS,
            externalizedKeyName = MAX_BUFFERS,
            defaultValue = "1024",
            description = "Max number of buffers for the interceptor. The default value is 1024. If the number of buffers exceeds this value, the large\n" +
                    "request body will be truncated. The buffer size is 16K, so the max size of the body can be intercepted is 16M. If you want to\n" +
                    "upload large file to the server with ExternalServiceHandler, you might need to increase the number of buffers to a larger value.\n" +
                    "Please be aware that the memory usage will be increased as well. So please use it with caution and test it with load test. Also,\n" +
                    "please make sure that you update the server.maxTransferFileSize to a larger value that matches the maxBuffers * 1024."
    )
    private int maxBuffers;
    private Map<String, Object> mappedConfig;
    private final Config config;

    private static volatile RequestInjectionConfig instance;

    private RequestInjectionConfig() {
        this(CONFIG_NAME);
    }

    private RequestInjectionConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfig(configName);
        setConfigData();
        setConfigList();
    }

    public static RequestInjectionConfig load() {
        return load(CONFIG_NAME);
    }

    public static RequestInjectionConfig load(String configName) {
        if (CONFIG_NAME.equals(configName)) {
            Map<String, Object> mappedConfig = Config.getInstance().getJsonMapConfig(configName);
            if (instance != null && instance.getMappedConfig() == mappedConfig) {
                return instance;
            }
            synchronized (RequestInjectionConfig.class) {
                mappedConfig = Config.getInstance().getJsonMapConfig(configName);
                if (instance != null && instance.getMappedConfig() == mappedConfig) {
                    return instance;
                }
                instance = new RequestInjectionConfig(configName);
                // Register the module with the configuration.
                ModuleRegistry.registerModule(configName, RequestInjectionConfig.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(configName), null);
                return instance;
            }
        }
        return new RequestInjectionConfig(configName);
    }

    public boolean isEnabled() {
        return enabled;
    }
    public int getMaxBuffers() {
        return maxBuffers;
    }
    public List<String> getAppliedBodyInjectionPathPrefixes() {
        return appliedBodyInjectionPathPrefixes;
    }

    Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    private void setConfigData() {
        var object = getMappedConfig().get(ENABLED);
        if (object != null) enabled = Config.loadBooleanValue(ENABLED, object);
        object = getMappedConfig().get(MAX_BUFFERS);
        if (object != null) maxBuffers = Config.loadIntegerValue(MAX_BUFFERS, object);
    }

    private void setConfigList() {
        if (this.mappedConfig != null && this.mappedConfig.get(APPLIED_BODY_INJECTION_PATH_PREFIXES) != null) {
            var object = this.mappedConfig.get(APPLIED_BODY_INJECTION_PATH_PREFIXES);
            this.appliedBodyInjectionPathPrefixes = new ArrayList<>();

            if (object instanceof String) {
                var s = (String) object;
                s = s.trim();

                if (LOG.isTraceEnabled())
                    LOG.trace("s = " + s);

                if (s.startsWith("[")) {
                    // json format
                    try {
                        this.appliedBodyInjectionPathPrefixes = Config.getInstance().getMapper().readValue(s, new TypeReference<>() {});
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the appliedBodyInjectionPathPrefixes json with a list of strings.");
                    }

                // comma separated
                } else this.appliedBodyInjectionPathPrefixes = Arrays.asList(s.split("\\s*,\\s*"));

            } else if (object instanceof List) {
                var prefixes = (List) object;
                prefixes.forEach(item -> {
                    this.appliedBodyInjectionPathPrefixes.add((String) item);
                });

            } else throw new ConfigException("appliedBodyInjectionPathPrefixes must be a string or a list of strings.");
        }
    }

}
