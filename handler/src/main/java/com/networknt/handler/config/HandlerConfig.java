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

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Nicholas Azar
 * @author Dan Dobrin
 */
public class HandlerConfig {
    public static final String CONFIG_NAME = "handler";
    private static final Logger logger = LoggerFactory.getLogger(HandlerConfig.class);
    private static final String ENABLED = "enabled";
    private static final String HANDLERS = "handlers";
    private static final String ADDITIONAL_HANDLERS = "additionalHandlers";
    private static final String CHAINS = "chains";
    private static final String ADDITIONAL_CHAINS = "additionalChains";
    private static final String PATHS = "paths";
    private static final String ADDITIONAL_PATHS = "additionalPaths";
    private static final String DEFAULT_HANDLERS = "defaultHandlers";
    private static final String AUDIT_ON_ERROR = "auditOnError";
    private static final String AUDIT_STACK_TRACE = "auditStackTrace";
    private static final String BASE_PATH = "basePath";

    private static final String PATH = "path";
    private static final String SOURCE = "source";
    private static final String EXEC = "exec";
    private static final String METHOD = "method";

    private boolean enabled;
    private List<String> handlers;
    private Map<String, List<String>> chains;
    private List<PathChain> paths;
    private List<String> defaultHandlers;
    private boolean auditOnError;
    private boolean auditStackTrace;
    private String basePath;
    private Map<String, Object> mappedConfig;
    private final Config config;

    private HandlerConfig() {
        this(CONFIG_NAME);
    }

    private HandlerConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setConfigList();
        setConfigMap();
    }

    public static HandlerConfig load() {
        return new HandlerConfig();
    }

    public static HandlerConfig load(String configName) {
        return new HandlerConfig(configName);
    }

    public void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
        setConfigList();
        setConfigMap();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getHandlers() {
        return handlers;
    }

    public void setHandlers(List<String> handlers) {
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


    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public boolean isAuditOnError() {
        return auditOnError;
    }

    public boolean isAuditStackTrace() {
        return auditStackTrace;
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    private void setConfigData() {
        if(mappedConfig != null) {
            Object object = mappedConfig.get(ENABLED);
            if(object != null) enabled = Config.loadBooleanValue(ENABLED, object);
            object = mappedConfig.get(AUDIT_ON_ERROR);
            if(object != null) auditOnError = Config.loadBooleanValue(AUDIT_ON_ERROR, object);
            object = mappedConfig.get(AUDIT_STACK_TRACE);
            if(object != null) auditStackTrace = Config.loadBooleanValue(AUDIT_STACK_TRACE, object);
            object = mappedConfig.get(BASE_PATH);
            if(object != null) basePath = (String)object;
        }
    }

    private void setConfigList() {
        if (mappedConfig != null && mappedConfig.get(HANDLERS) != null) {
            Object object = mappedConfig.get(HANDLERS);
            handlers = new ArrayList<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("s = " + s);
                if(s.startsWith("[")) {
                    // json format
                    try {
                        handlers = Config.getInstance().getMapper().readValue(s, new TypeReference<>() {});
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the handlers json with a list of strings.");
                    }
                } else {
                    // comma separated
                    handlers = Arrays.asList(s.split("\\s*,\\s*"));
                }
            } else if (object instanceof List) {
                List<String> list = (List)object;
                handlers.addAll(list);
            } else {
                throw new ConfigException("handlers must be a string or a list of strings.");
            }
            // add additional handlers to the handlers if exist
            if (mappedConfig.get(ADDITIONAL_HANDLERS) != null) {
                object = mappedConfig.get(ADDITIONAL_HANDLERS);
                if(object instanceof String) {
                    String s = (String)object;
                    s = s.trim();
                    if(logger.isTraceEnabled()) logger.trace("s = " + s);
                    if(s.startsWith("[")) {
                        // json format
                        try {
                            handlers.addAll(Config.getInstance().getMapper().readValue(s, new TypeReference<>() {}));
                        } catch (Exception e) {
                            throw new ConfigException("could not parse the additionalHandlers json with a list of strings.");
                        }
                    } else {
                        // comma separated
                        handlers.addAll(Arrays.asList(s.split("\\s*,\\s*")));
                    }
                } else if (object instanceof List) {
                    List<String> list = (List)object;
                    handlers.addAll(list);
                } else {
                    throw new ConfigException("additionalHandlers must be a string or a list of strings.");
                }
            }
        }
        if (mappedConfig != null && mappedConfig.get(DEFAULT_HANDLERS) != null) {
            Object object = mappedConfig.get(DEFAULT_HANDLERS);
            defaultHandlers = new ArrayList<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("s = " + s);
                if(s.startsWith("[")) {
                    // json format
                    try {
                        defaultHandlers = Config.getInstance().getMapper().readValue(s, new TypeReference<>() {});
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the defaultHandlers json with a list of strings.");
                    }
                } else {
                    // comma separated
                    defaultHandlers = Arrays.asList(s.split("\\s*,\\s*"));
                }
            } else if (object instanceof List) {
                List<String> list = (List)object;
                defaultHandlers.addAll(list);
            } else {
                throw new ConfigException("defaultHandlers must be a string or a list of strings.");
            }
        }
        if (mappedConfig != null && mappedConfig.get(PATHS) != null) {
            Object object = mappedConfig.get(PATHS);
            paths = new ArrayList<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("paths s = " + s);
                if(s.startsWith("[")) {
                    // json format
                    try {
                        paths = Config.getInstance().getMapper().readValue(s, new TypeReference<List<PathChain>>() {});
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the paths json with a list of string and object.");
                    }
                } else {
                    throw new ConfigException("paths must be a list of string object map.");
                }
            } else if (object instanceof List) {
                // the object is a list of map, we need convert it to PathPrefixAuth object.
                List<Map<String, Object>> values = (List<Map<String, Object>>)object;
                for(Map<String, Object> value: values) {
                    PathChain pathChain = new PathChain();
                    pathChain.setPath((String)value.get(PATH));
                    pathChain.setSource((String)value.get(SOURCE));
                    pathChain.setMethod((String)value.get(METHOD));
                    pathChain.setExec((List<String>)value.get(EXEC));
                    paths.add(pathChain);
                }
            } else {
                throw new ConfigException("paths must be a list of string object map.");
            }
            // add additional paths to the paths if exist
            if (mappedConfig.get(ADDITIONAL_PATHS) != null) {
                object = mappedConfig.get(ADDITIONAL_PATHS);
                if(object instanceof String) {
                    String s = (String)object;
                    s = s.trim();
                    if(logger.isTraceEnabled()) logger.trace("s = " + s);
                    if(s.startsWith("[")) {
                        // json format
                        try {
                            paths.addAll(Config.getInstance().getMapper().readValue(s, new TypeReference<List<PathChain>>() {}));
                        } catch (Exception e) {
                            throw new ConfigException("could not parse the additionalPaths json with a list of string and object.");
                        }
                    } else {
                        throw new ConfigException("additionalPaths must be a list of string object map.");
                    }
                } else if (object instanceof List) {
                    // the object is a list of map, we need convert it to PathPrefixAuth object.
                    List<Map<String, Object>> values = (List<Map<String, Object>>)object;
                    for(Map<String, Object> value: values) {
                        PathChain pathChain = new PathChain();
                        pathChain.setPath((String)value.get(PATH));
                        pathChain.setSource((String)value.get(SOURCE));
                        pathChain.setMethod((String)value.get(METHOD));
                        pathChain.setExec((List<String>)value.get(EXEC));
                        paths.add(pathChain);
                    }
                } else {
                    throw new ConfigException("additionalPaths must be a list of string object map.");
                }
            }
        }

    }

    private void setConfigMap() {
        if (mappedConfig != null && mappedConfig.get(CHAINS) != null) {
            Object chainsObject = mappedConfig.get(CHAINS);
            if(chainsObject != null) {
                if(chainsObject instanceof String) {
                    String s = (String)chainsObject;
                    s = s.trim();
                    if(logger.isTraceEnabled()) logger.trace("chains s = " + s);
                    if(s.startsWith("{")) {
                        // json format
                        try {
                            chains = Config.getInstance().getMapper().readValue(s, new TypeReference<Map<String, List<String>>>() {});
                        } catch (Exception e) {
                            throw new ConfigException("could not parse the chains json with a map of string and list of strings.");
                        }
                    } else {
                        throw new ConfigException("could not parse the chains json with a map of string and list of strings.");
                    }
                } else if (chainsObject instanceof Map) {
                    chains = new HashMap<>();
                    // the map value can be a list of string or a json of list of string. need to convert into list of string.
                    Map<String, Object> map = (Map)chainsObject;
                    for (Map.Entry<String, Object> entry : map.entrySet()) {
                        String key = entry.getKey();
                        Object value = entry.getValue();
                        if (value instanceof String) {
                            // convert to list of strings
                            List<String> list;
                            String s = (String) value;
                            s = s.trim();
                            if (logger.isTraceEnabled()) logger.trace("s = " + s);
                            if (s.startsWith("[")) {
                                // json format
                                try {
                                    list = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {
                                    });
                                } catch (Exception e) {
                                    throw new ConfigException("could not parse the chains json with a map of string and list of strings.");
                                }
                            } else {
                                // comma separated
                                list = Arrays.asList(s.split("\\s*,\\s*"));
                            }
                            chains.put(key, list);
                        } else if (value instanceof List) {
                            // do nothing
                            chains.put(key, (List) value);
                        } else {
                            throw new ConfigException("chains must be a string object map.");
                        }
                    }
                } else {
                    throw new ConfigException("chains must be a string object map.");
                }
                // add additional chains to the chains if exist
                if (mappedConfig.get(ADDITIONAL_CHAINS) != null) {
                    chainsObject = mappedConfig.get(ADDITIONAL_CHAINS);
                    if(chainsObject instanceof String) {
                        String s = (String)chainsObject;
                        s = s.trim();
                        if(logger.isTraceEnabled()) logger.trace("s = " + s);
                        if(s.startsWith("{")) {
                            // json format
                            try {
                                chains.putAll(Config.getInstance().getMapper().readValue(s, new TypeReference<Map<String, List<String>>>() {}));
                            } catch (Exception e) {
                                throw new ConfigException("could not parse the additionalChains json with a map of string and list of strings.");
                            }
                        } else {
                            throw new ConfigException("additionalChains must be a string object map.");
                        }
                    } else if (chainsObject instanceof Map) {
                        Map<String, Object> map = (Map)chainsObject;
                        for (Map.Entry<String, Object> entry : map.entrySet()) {
                            String key = entry.getKey();
                            Object value = entry.getValue();
                            if (value instanceof String) {
                                // convert to list of strings
                                List<String> list;
                                String s = (String) value;
                                s = s.trim();
                                if (logger.isTraceEnabled()) logger.trace("s = " + s);
                                if (s.startsWith("[")) {
                                    // json format
                                    try {
                                        list = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {
                                        });
                                    } catch (Exception e) {
                                        throw new ConfigException("could not parse the additionalChains json with a map of string and list of strings.");
                                    }
                                } else {
                                    // comma separated
                                    list = Arrays.asList(s.split("\\s*,\\s*"));
                                }
                                chains.put(key, list);
                            } else if (value instanceof List) {
                                // do nothing
                                chains.put(key, (List) value);
                            } else {
                                throw new ConfigException("additionalChains must be a string object map.");
                            }
                        }
                    } else {
                        throw new ConfigException("additionalChains must be a string object map.");
                    }
                }
            }

        }
    }
}
