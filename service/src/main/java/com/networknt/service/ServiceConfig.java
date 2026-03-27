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

package com.networknt.service;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Service Config Class that encapsulate all the service defined in service.yml
 *
 * @author Steve Hu
 */
public class ServiceConfig {
    private static final Logger logger = LoggerFactory.getLogger(ServiceConfig.class);

    /** Constant for configuration name */
    public static final String CONFIG_NAME = "service";

    /** Constant for singletons configuration entry */
    public static final String SINGLETONS = "singletons";
    private List<Map<String, Object>> singletons;
    private Map<String, Object> mappedConfig;

    private ServiceConfig() {
        this(CONFIG_NAME);
    }

    private ServiceConfig(String configName) {
        mappedConfig = Config.getInstance().getJsonMapConfig(configName);
        setConfigData();
    }

    /**
     * Loads the service configuration from the default config name.
     *
     * @return ServiceConfig object
     */
    public static ServiceConfig load() {
        return new ServiceConfig();
    }

    /**
     * Loads the service configuration from a specific config name.
     *
     * @param configName config name
     * @return ServiceConfig object
     */
    public static ServiceConfig load(String configName) {
        return new ServiceConfig(configName);
    }

    /**
     * Gets the mapped configuration.
     *
     * @return Map of configuration entries
     */
    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    /**
     * Gets the list of singleton service definitions.
     *
     * @return List of maps
     */
    public List<Map<String, Object>> getSingletons() {
        return singletons;
    }

    /**
     * Sets the configuration data from the mapped config.
     */
    public void setConfigData() {
        if(mappedConfig.get(SINGLETONS) instanceof String) {
            // the json string is supported here.
            String s = (String)mappedConfig.get(SINGLETONS);
            if(logger.isTraceEnabled()) logger.trace("singletons = " + s);
            singletons = JsonMapper.string2List(s);
        } else if (mappedConfig.get(SINGLETONS) instanceof List) {
            singletons = (List<Map<String, Object>>)mappedConfig.get(SINGLETONS);
        } else {
            if(logger.isInfoEnabled()) logger.info("singletons missing or wrong type.");
            // ignore this situation as a particular application might not have any injections.
        }
    }
}
