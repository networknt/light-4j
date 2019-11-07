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

package com.networknt.handler.config;

import java.util.List;
import java.util.Map;

/**
 * @author Nicholas Azar
 * @author Dan Dobrin
 */
public class HandlerConfig {
    private boolean enabled;
    private List<Object> handlers;
    private Map<String, List<String>> chains;
    private List<PathChain> paths;
    private List<String> defaultHandlers;
    private boolean auditOnError;
    private boolean auditStackTrace;

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

    public List<String> getDefaultHandlers() {
        return defaultHandlers;
    }

    public void setDefaultHandlers(List<String> defaultHandlers) {
        this.defaultHandlers = defaultHandlers;
    }
    
    public boolean getAuditOnError() {
    	return auditOnError;
    }
    
    public void setAuditOnError(boolean auditOnError) {
    	this.auditOnError = auditOnError;
    }
    
    public boolean getAuditStackTrace() {
    	return auditStackTrace;
    }
    
    public void setAuditStackTrace(boolean auditStackTrace) {
    	this.auditStackTrace = auditStackTrace;
    }
}

