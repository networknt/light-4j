package com.networknt.handler.config;

import java.util.List;

/**
 * @author Nicholas Azar
 */
public class HandlerConfig {
    private boolean enabled;
    private List<PathHandler> pathHandlers;
    private List<NamedRequestChain> namedRequestChain;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<PathHandler> getPathHandlers() {
        return pathHandlers;
    }

    public void setPathHandlers(List<PathHandler> pathHandlers) {
        this.pathHandlers = pathHandlers;
    }

    public List<NamedRequestChain> getNamedRequestChain() {
        return namedRequestChain;
    }

    public void setNamedRequestChain(List<NamedRequestChain> namedRequestChain) {
        this.namedRequestChain = namedRequestChain;
    }
}
