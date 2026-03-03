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
import com.networknt.config.schema.*;
import com.networknt.server.ModuleRegistry;

import java.util.Map;

@ConfigSchema(
        configKey = "path-resource",
        configName = "path-resource",
        configDescription = "Path Resource Config",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD}
)
public class PathResourceConfig {
    public static final String CONFIG_NAME = "path-resource";

    @StringField(
            configFieldName = "path",
            externalizedKeyName = "path",
            description = "The path of the resource",
            defaultValue = "/public"
    )
    String path;

    @StringField(
            configFieldName = "base",
            externalizedKeyName = "base",
            description = "The base directory of the resource",
            defaultValue = "/opt/light-4j/public"
    )
    String base;

    @BooleanField(
            configFieldName = "prefix",
            externalizedKeyName = "prefix",
            description = "If true, the path is a prefix",
            defaultValue = "true"
    )
    boolean prefix;

    @IntegerField(
            configFieldName = "transferMinSize",
            externalizedKeyName = "transferMinSize",
            description = "The minimum size of the file to be transferred",
            defaultValue = "1024"
    )
    int transferMinSize;

    @BooleanField(
            configFieldName = "directoryListingEnabled",
            externalizedKeyName = "directoryListingEnabled",
            description = "If true, directory listing is enabled",
            defaultValue = "false"
    )
    boolean directoryListingEnabled;

    private static volatile PathResourceConfig instance;
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
        return load(CONFIG_NAME);
    }

    public static PathResourceConfig load(String configName) {
        if (CONFIG_NAME.equals(configName)) {
            if (instance != null && instance.getMappedConfig() == Config.getInstance().getJsonMapConfig(configName)) {
                return instance;
            }
            synchronized (PathResourceConfig.class) {
                if (instance != null && instance.getMappedConfig() == Config.getInstance().getJsonMapConfig(configName)) {
                    return instance;
                }
                instance = new PathResourceConfig(configName);
                ModuleRegistry.registerModule(CONFIG_NAME, PathResourceConfig.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(CONFIG_NAME), null);
                return instance;
            }
        }
        return new PathResourceConfig(configName);
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
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
