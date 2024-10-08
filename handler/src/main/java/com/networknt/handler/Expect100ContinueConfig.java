package com.networknt.handler;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;

import java.util.List;
import java.util.Map;

public class Expect100ContinueConfig {
    public static final String CONFIG_NAME = "expect-100-continue";
    private static final String ENABLED = "enabled";
    private static final String IGNORED_PATHS = "ignoredPaths";
    private boolean enabled;
    private List<String> ignoredPaths;
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
            this.enabled = (Boolean)this.mappedConfig.get(ENABLED);

        if (this.mappedConfig.containsKey(IGNORED_PATHS)) {

            if (this.mappedConfig.get(IGNORED_PATHS) instanceof List)
                this.ignoredPaths = (List)this.mappedConfig.get(IGNORED_PATHS);

            else if (this.mappedConfig.get(IGNORED_PATHS) instanceof String) {

                final var ignoredPathsString = ((String)this.mappedConfig.get(IGNORED_PATHS)).trim();

                if (!ignoredPathsString.isEmpty()
                        && !ignoredPathsString.isBlank()
                        && ignoredPathsString.contains("["))
                    this.ignoredPaths = List.of(ignoredPathsString
                            .trim()
                            .replace("[", "")
                            .replace("]", "")
                            .split(",")
                    );

            } else throw new ConfigException("'ignoredPaths' must be a list or a string");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<String> getIgnoredPaths() {
        return ignoredPaths;
    }
}
