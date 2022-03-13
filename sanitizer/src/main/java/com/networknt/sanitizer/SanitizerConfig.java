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

package com.networknt.sanitizer;

import com.networknt.config.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Sanitizer configuration class
 *
 * @author Steve Hu
 */
public class SanitizerConfig {
    public static final String CONFIG_NAME = "sanitizer";
    private boolean enabled;
    private boolean bodyEnabled;
    private String bodyEncoder;
    private List<String> bodyAttributesToIgnore;
    private List<String> bodyAttributesToEncode;

    private boolean headerEnabled;
    private String headerEncoder;
    private List<String> headerAttributesToIgnore;
    private List<String> headerAttributesToEncode;

    private final Map<String, Object> mappedConfig;

    private SanitizerConfig(String configName) {
        mappedConfig = Config.getInstance().getJsonMapConfig(configName);
        setBodyAttributesToEncodeList();
        setBodyAttributesToIgnoreList();
        setHeaderAttributesToEncodeList();
        setHeaderAttributesToIgnoreList();
        setConfigData();
    }

    public static SanitizerConfig load() {
        return new SanitizerConfig(CONFIG_NAME);
    }

    @Deprecated
    public static SanitizerConfig load(String configName) {
        return new SanitizerConfig(configName);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isBodyEnabled() {
        return bodyEnabled;
    }

    public void setBodyEnabled(boolean bodyEnabled) {
        this.bodyEnabled = bodyEnabled;
    }

    public String getBodyEncoder() {
        return bodyEncoder;
    }

    public void setBodyEncoder(String bodyEncoder) {
        this.bodyEncoder = bodyEncoder;
    }

    public List<String> getBodyAttributesToIgnore() {
        return bodyAttributesToIgnore;
    }

    public void setBodyAttributesToIgnore(List<String> bodyAttributesToIgnore) {
        this.bodyAttributesToIgnore = bodyAttributesToIgnore;
    }

    public List<String> getBodyAttributesToEncode() {
        return bodyAttributesToEncode;
    }

    public void setBodyAttributesToEncode(List<String> bodyAttributesToEncode) {
        this.bodyAttributesToEncode = bodyAttributesToEncode;
    }

    public boolean isHeaderEnabled() {
        return headerEnabled;
    }

    public void setHeaderEnabled(boolean headerEnabled) {
        this.headerEnabled = headerEnabled;
    }

    public String getHeaderEncoder() {
        return headerEncoder;
    }

    public void setHeaderEncoder(String headerEncoder) {
        this.headerEncoder = headerEncoder;
    }

    public List<String> getHeaderAttributesToIgnore() {
        return headerAttributesToIgnore;
    }

    public void setHeaderAttributesToIgnore(List<String> headerAttributesToIgnore) {
        this.headerAttributesToIgnore = headerAttributesToIgnore;
    }

    public List<String> getHeaderAttributesToEncode() {
        return headerAttributesToEncode;
    }

    public void setHeaderAttributesToEncode(List<String> headerAttributesToEncode) {
        this.headerAttributesToEncode = headerAttributesToEncode;
    }

    public void setConfigData() {
        Object object = mappedConfig.get("enabled");
        if(object != null && (Boolean) object) {
            enabled = true;
        }

        object = mappedConfig.get("bodyEnabled");
        if(object != null && (Boolean) object) {
            bodyEnabled = true;
        }

        object = mappedConfig.get("headerEnabled");
        if(object != null && (Boolean) object) {
            headerEnabled = true;
        }

        object = mappedConfig.get("bodyEncoder");
        if(object != null ) {
            bodyEncoder = (String)object;
        }

        object = mappedConfig.get("headerEncoder");
        if(object != null ) {
            headerEncoder = (String)object;
        }
    }

    public void setBodyAttributesToEncodeList() {
        this.bodyAttributesToEncode = new ArrayList<>();
        if(mappedConfig.get("bodyAttributesToEncode") != null && mappedConfig.get("bodyAttributesToEncode") instanceof String) {
            bodyAttributesToEncode.add((String)mappedConfig.get("bodyAttributesToEncode"));
        } else {
            bodyAttributesToEncode = (List)mappedConfig.get("bodyAttributesToEncode");
        }
    }

    public void setBodyAttributesToIgnoreList() {
        this.bodyAttributesToIgnore = new ArrayList<>();
        if(mappedConfig.get("bodyAttributesToIgnore") != null && mappedConfig.get("bodyAttributesToIgnore") instanceof String) {
            bodyAttributesToIgnore.add((String)mappedConfig.get("bodyAttributesToIgnore"));
        } else {
            bodyAttributesToIgnore = (List)mappedConfig.get("bodyAttributesToIgnore");
        }
    }

    public void setHeaderAttributesToEncodeList() {
        this.headerAttributesToEncode = new ArrayList<>();
        if(mappedConfig.get("headerAttributesToEncode") != null && mappedConfig.get("headerAttributesToEncode") instanceof String) {
            headerAttributesToEncode.add((String)mappedConfig.get("headerAttributesToEncode"));
        } else {
            headerAttributesToEncode = (List)mappedConfig.get("headerAttributesToEncode");
        }
    }

    public void setHeaderAttributesToIgnoreList() {
        this.headerAttributesToIgnore = new ArrayList<>();
        if(mappedConfig.get("headerAttributesToIgnore") != null && mappedConfig.get("headerAttributesToIgnore") instanceof String) {
            headerAttributesToIgnore.add((String)mappedConfig.get("headerAttributesToIgnore"));
        } else {
            headerAttributesToIgnore = (List)mappedConfig.get("headerAttributesToIgnore");
        }
    }

}
