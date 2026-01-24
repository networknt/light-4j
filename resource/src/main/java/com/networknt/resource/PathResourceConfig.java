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

package com.networknt.resource;
import com.networknt.config.Config;

public class PathResourceConfig {
    public static final String CONFIG_NAME = "path-resource";

    String path;
    String base;
    boolean prefix;
    int transferMinSize;
    boolean directoryListingEnabled;

    private final Config config;
    private java.util.Map<String, Object> mappedConfig;

    private PathResourceConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfig(configName);
        if (mappedConfig != null) {
            setConfigData();
        }
    }

    private PathResourceConfig() {
        this(CONFIG_NAME);
    }

    public static PathResourceConfig load() {
        return new PathResourceConfig();
    }

    public static PathResourceConfig load(String configName) {
        return new PathResourceConfig(configName);
    }

    private void setConfigData() {
        if (mappedConfig.containsKey("path")) {
            path = (String) mappedConfig.get("path");
        }
        if (mappedConfig.containsKey("base")) {
            base = (String) mappedConfig.get("base");
        }
        if (mappedConfig.containsKey("prefix")) {
            prefix = Config.loadBooleanValue("prefix", mappedConfig.get("prefix"));
        }
        if (mappedConfig.containsKey("transferMinSize")) {
            transferMinSize = Config.loadIntegerValue("transferMinSize", mappedConfig.get("transferMinSize"));
        }
        if (mappedConfig.containsKey("directoryListingEnabled")) {
            directoryListingEnabled = Config.loadBooleanValue("directoryListingEnabled", mappedConfig.get("directoryListingEnabled"));
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public boolean isPrefix() {
        return prefix;
    }

    public void setPrefix(boolean prefix) {
        this.prefix = prefix;
    }

    public int getTransferMinSize() {
        return transferMinSize;
    }

    public void setTransferMinSize(int transferMinSize) {
        this.transferMinSize = transferMinSize;
    }

    public boolean isDirectoryListingEnabled() {
        return directoryListingEnabled;
    }

    public void setDirectoryListingEnabled(boolean directoryListingEnabled) {
        this.directoryListingEnabled = directoryListingEnabled;
    }

}
