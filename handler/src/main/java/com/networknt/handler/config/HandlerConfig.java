package com.networknt.handler.config;

import java.util.List;
import java.util.Map;

/**
 * @author Nicholas Azar
 */
public class HandlerConfig {
    private boolean enabled;
    private List<Object> handlers;
    private Map<String, List<String>> chains;
    private List<PathChain> paths;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<Object> getHandlers() {
        return handlers;
    }

    public void setHandlers(List<Object> handlers) {
        this.handlers = handlers;
    }

    public Map<String, List<String>> getChains() {
        return chains;
    }

    public void setChains(Map<String, List<String>> chains) {
        this.chains = chains;
    }

    public List<PathChain> getPaths() {
        return paths;
    }

    public void setPaths(List<PathChain> paths) {
        this.paths = paths;
    }
}

