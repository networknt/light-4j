package com.networknt.header;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class HeaderConfig {
    public static Logger logger = LoggerFactory.getLogger(HeaderConfig.class);

    public static final String CONFIG_NAME = "header";
    public static final String ENABLED = "enabled";
    public static final String REQUEST = "request";
    public static final String RESPONSE = "response";
    public static final String REMOVE = "remove";
    public static final String UPDATE = "update";
    public static final String PATH_PREFIX_HEADER = "pathPrefixHeader";

    boolean enabled;
    List<String> requestRemoveList;
    Map<String, Object> requestUpdateMap;

    List<String> responseRemoveList;
    Map<String, Object> responseUpdateMap;
    Map<String, Object> pathPrefixHeader;
    private Config config;
    private Map<String, Object> mappedConfig;

    private HeaderConfig() {
        this(CONFIG_NAME);
    }

    /**
     * Please note that this constructor is only for testing to load different config files
     * to test different configurations.
     * @param configName String
     */
    private HeaderConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setConfigList();
        setConfigMap();
    }

    public static HeaderConfig load() {
        return new HeaderConfig();
    }

    public static HeaderConfig load(String configName) {
        return new HeaderConfig(configName);
    }

    void reload() {
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

    public Map<String, Object> getPathPrefixHeader() {
        return pathPrefixHeader;
    }

    public void setPathPrefixHeader(Map<String, Object> pathPrefixHeader) {
        this.pathPrefixHeader = pathPrefixHeader;
    }

    private void setConfigData() {
        Object object = mappedConfig.get(ENABLED);
        if (object != null && (Boolean) object) {
            setEnabled((Boolean)object);
        }
    }

    public List<String> getRequestRemoveList() {
        return requestRemoveList;
    }

    public void setRequestRemoveList(List<String> requestRemoveList) {
        this.requestRemoveList = requestRemoveList;
    }

    public Map<String, Object> getRequestUpdateMap() {return requestUpdateMap; }

    public void setRequestUpdateMap(Map<String, Object> requestUpdateMap) {
        this.requestUpdateMap = requestUpdateMap;
    }

    public List<String> getResponseRemoveList() {
        return responseRemoveList;
    }

    public void setResponseRemoveList(List<String> responseRemoveList) {
        this.responseRemoveList = responseRemoveList;
    }

    public Map<String, Object> getResponseUpdateMap() {return responseUpdateMap; }

    public void setResponseUpdateMap(Map<String, Object> responseUpdateMap) {
        this.responseUpdateMap = responseUpdateMap;
    }

    private void setConfigList() {
        if (mappedConfig.get(REQUEST) != null) {
            Map<String, Object> requestMap = (Map<String, Object>)mappedConfig.get(REQUEST);
            Object requestRemove = requestMap.get(REMOVE);
            if(requestRemove != null) {
                if(requestRemove instanceof String) {
                    String s = (String)requestRemove;
                    s = s.trim();
                    if(logger.isTraceEnabled()) logger.trace("request remove s = " + s);
                    if(s.startsWith("[")) {
                        // this is a JSON string, and we need to parse it.
                        try {
                            requestRemoveList = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {});
                        } catch (Exception e) {
                            throw new ConfigException("could not parse the request.remove json with a list of strings.");
                        }
                    } else {
                        // this is a comma separated string.
                        requestRemoveList = Arrays.asList(s.split("\\s*,\\s*"));
                    }
                } else if (requestRemove instanceof List) {
                    requestRemoveList = (List<String>)requestRemove;
                } else {
                    throw new ConfigException("request remove list is missing or wrong type.");
                }
            }
        }

        if (mappedConfig.get(RESPONSE) != null) {
            Map<String, Object> responseMap = (Map<String, Object>)mappedConfig.get(RESPONSE);
            Object responseRemove = responseMap.get(REMOVE);
            if(responseRemove != null) {
                if(responseRemove instanceof String) {
                    String s = (String)responseRemove;
                    s = s.trim();
                    if(logger.isTraceEnabled()) logger.trace("response remove s = " + s);
                    if(s.startsWith("[")) {
                        // this is a JSON string, and we need to parse it.
                        try {
                            responseRemoveList = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {});
                        } catch (Exception e) {
                            throw new ConfigException("could not parse the response.remove json with a list of strings.");
                        }
                    } else {
                        // this is a comma separated string.
                        responseRemoveList = Arrays.asList(s.split("\\s*,\\s*"));
                    }
                } else if (responseRemove instanceof List) {
                    responseRemoveList = (List<String>)responseRemove;
                } else {
                    throw new ConfigException("response remove list is missing or wrong type.");
                }
            }
        }
    }

    private void setConfigMap() {
        if (mappedConfig.get(REQUEST) != null) {
            Map<String, Object> requestMap = (Map<String, Object>)mappedConfig.get(REQUEST);
            Object requestUpdate = requestMap.get(UPDATE);
            if(requestUpdate != null) {
                if(requestUpdate instanceof String) {
                    String s = (String)requestUpdate;
                    s = s.trim();
                    if(logger.isTraceEnabled()) logger.trace("request update s = " + s);
                    if(s.startsWith("{")) {
                        // json format
                        try {
                            requestUpdateMap = JsonMapper.string2Map(s);
                        } catch (Exception e) {
                            throw new ConfigException("could not parse the request.update json with a map of string and object.");
                        }
                    } else {
                        // comma separated
                        requestUpdateMap = new HashMap<>();
                        String[] pairs = s.split(",");
                        for (int i = 0; i < pairs.length; i++) {
                            String pair = pairs[i];
                            String[] keyValue = pair.split(":");
                            requestUpdateMap.put(keyValue[0], keyValue[1]);
                        }
                    }
                } else if (requestUpdate instanceof Map) {
                    requestUpdateMap = (Map)requestUpdate;
                } else {
                    throw new ConfigException("request update must be a string object map.");
                }
            }
        }

        if (mappedConfig.get(RESPONSE) != null) {
            Map<String, Object> responseMap = (Map<String, Object>)mappedConfig.get(RESPONSE);
            Object responseUpdate = responseMap.get(UPDATE);
            if(responseUpdate != null) {
                if(responseUpdate instanceof String) {
                    String s = (String)responseUpdate;
                    s = s.trim();
                    if(logger.isTraceEnabled()) logger.trace("response update s = " + s);
                    if(s.startsWith("{")) {
                        // json format
                        try {
                            responseUpdateMap = JsonMapper.string2Map(s);
                        } catch (Exception e) {
                            throw new ConfigException("could not parse the response.update json with a map of string and object.");
                        }
                    } else {
                        // comma separated
                        responseUpdateMap = new HashMap<>();
                        String[] pairs = s.split(",");
                        for (int i = 0; i < pairs.length; i++) {
                            String pair = pairs[i];
                            String[] keyValue = pair.split(":");
                            responseUpdateMap.put(keyValue[0], keyValue[1]);
                        }
                    }
                } else if (responseUpdate instanceof Map) {
                    responseUpdateMap = (Map)responseUpdate;
                } else {
                    throw new ConfigException("response update must be a string object map.");
                }
            }
        }

        // load pathPrefixHeader here.
        if(mappedConfig.get(PATH_PREFIX_HEADER) != null) {
            Object pathPrefixHeaderObj = mappedConfig.get(PATH_PREFIX_HEADER);
            if(pathPrefixHeaderObj != null) {
                if(pathPrefixHeaderObj instanceof String) {
                    String s = (String)pathPrefixHeaderObj;
                    s = s.trim();
                    if(logger.isTraceEnabled()) logger.trace("pathPrefixHeader s = " + s);
                    if(s.startsWith("{")) {
                        // json format
                        try {
                            pathPrefixHeader = JsonMapper.string2Map(s);
                        } catch (Exception e) {
                            throw new ConfigException("could not parse the pathPrefixHeader json with a map of string and object.");
                        }
                    } else {
                        // comma separated
                        pathPrefixHeader = new HashMap<>();
                        String[] pairs = s.split(",");
                        for (int i = 0; i < pairs.length; i++) {
                            String pair = pairs[i];
                            String[] keyValue = pair.split(":");
                            pathPrefixHeader.put(keyValue[0], keyValue[1]);
                        }
                    }
                } else if (pathPrefixHeaderObj instanceof Map) {
                    pathPrefixHeader = (Map)pathPrefixHeaderObj;
                } else {
                    throw new ConfigException("pathPrefixHeader must be a string object map.");
                }
            }
        }
    }
}
