package com.networknt.handler;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Expect100ContinueConfig {
    public static final String CONFIG_NAME = "expect-100-continue";
    private static final String ENABLED = "enabled";
    private static final String IGNORED_PATH_PREFIXES = "ignoredPathPrefixes";
    private boolean enabled;
    private List<String> ignoredPathPrefixes;
    private final Config config;
    private Map<String, Object> mappedConfig;

    public static Expect100ContinueConfig load() {
        return new Expect100ContinueConfig();
    }

    private Expect100ContinueConfig() {
        this(CONFIG_NAME);
    }

    private Expect100ContinueConfig(String configName) {
        this.config = Config.getInstance();
        this.mappedConfig = config.getJsonMapConfigNoCache(configName);
        this.setConfigData();
    }

    void reload() {
        this.mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        this.setConfigData();
    }

    private void setConfigData() {

        if (this.mappedConfig.containsKey(ENABLED))
            this.enabled = (Boolean) this.mappedConfig.get(ENABLED);

        if (this.mappedConfig.containsKey(IGNORED_PATH_PREFIXES)) {

            if (this.mappedConfig.get(IGNORED_PATH_PREFIXES) == null)
                this.ignoredPathPrefixes = new ArrayList<>();

            else if (this.mappedConfig.get(IGNORED_PATH_PREFIXES) instanceof List)
                this.ignoredPathPrefixes = (List) this.mappedConfig.get(IGNORED_PATH_PREFIXES);

            else if (this.mappedConfig.get(IGNORED_PATH_PREFIXES) instanceof String) {

                final var ignoredPathsString = ((String) this.mappedConfig.get(IGNORED_PATH_PREFIXES)).trim();

                if (!ignoredPathsString.isEmpty()
                        && !ignoredPathsString.isBlank()
                        && ignoredPathsString.contains("["))
                    this.ignoredPathPrefixes = List.of(ignoredPathsString
                            .trim()
                            .replace("[", "")
                            .replace("]", "")
                            .replace(" ", "")
                            .split(",")
                    );

            } else throw new ConfigException("'ignoredPaths' must be a list or a string");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<String> getIgnoredPathPrefixes() {
        return ignoredPathPrefixes;
    }
}
