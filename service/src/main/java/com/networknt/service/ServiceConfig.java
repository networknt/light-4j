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

    public static final String CONFIG_NAME = "service";
    public static final String SINGLETONS = "singletons";
    private List<Map<String, Object>> singletons;
    private Config config;
    private Map<String, Object> mappedConfig;

    private ServiceConfig() {
        this(CONFIG_NAME);
    }

    private ServiceConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
    }

    public static ServiceConfig load() {
        return new ServiceConfig();
    }

    public static ServiceConfig load(String configName) {
        return new ServiceConfig(configName);
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    public List<Map<String, Object>> getSingletons() {
        return singletons;
    }

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
