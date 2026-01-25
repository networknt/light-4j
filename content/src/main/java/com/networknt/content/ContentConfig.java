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

package com.networknt.content;

import com.networknt.config.Config;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.OutputFormat;
import com.networknt.config.schema.StringField;
import com.networknt.server.ModuleRegistry;

import java.util.Map;

/**
 * Created by Ricardo Pina Arellano on 13/06/18.
 */
@ConfigSchema(
        configKey = "content",
        configName = "content",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD},
        configDescription = "Content middleware configuration."
)
public class ContentConfig {
  public static final String CONFIG_NAME = "content";
  private static final String ENABLED = "enabled";
  private static final String CONTENT_TYPE = "contentType";
  private Map<String, Object> mappedConfig;
  private final Config config;

  @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            description = "Indicate if the content middleware is enabled or not.",
            defaultValue = "true"
  )
  private boolean enabled;

  @StringField(
            configFieldName = CONTENT_TYPE,
            externalizedKeyName = CONTENT_TYPE,
            pattern = ".*/.*",
            description = "The content type to be used in the response.",
            defaultValue = "application/json"
  )
  private String contentType;

  private static volatile ContentConfig instance;

  private ContentConfig(String configName) {
    config = Config.getInstance();
    mappedConfig = config.getJsonMapConfig(configName);
    setConfigData();
  }
  private ContentConfig() {
    this(CONFIG_NAME);
  }

  public static ContentConfig load() {
    return load(CONFIG_NAME);
  }

  public static ContentConfig load(String configName) {
    if (CONFIG_NAME.equals(configName)) {
      Map<String, Object> mappedConfig = Config.getInstance().getJsonMapConfig(configName);
      if (instance != null && instance.getMappedConfig() == mappedConfig) {
        return instance;
      }
      synchronized (ContentConfig.class) {
        mappedConfig = Config.getInstance().getJsonMapConfig(configName);
        if (instance != null && instance.getMappedConfig() == mappedConfig) {
          return instance;
        }
        instance = new ContentConfig(configName);
        // Register the module with the configuration.
        ModuleRegistry.registerModule(configName, ContentConfig.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(configName), null);
        return instance;
      }
    }
    return new ContentConfig(configName);
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public Map<String, Object> getMappedConfig() {
    return mappedConfig;
  }

  Config getConfig() {
    return config;
  }

  private void setConfigData() {
    if(getMappedConfig() != null) {
      Object object = getMappedConfig().get(ENABLED);
      if(object != null) enabled = Config.loadBooleanValue(ENABLED, object);
      object = getMappedConfig().get(CONTENT_TYPE);
      if(object != null) contentType = (String)object;
    }
  }
}
