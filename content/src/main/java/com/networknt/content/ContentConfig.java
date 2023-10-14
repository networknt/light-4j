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
import com.networknt.config.ConfigException;

import java.util.Map;

/**
 * Created by Ricardo Pina Arellano on 13/06/18.
 */
public class ContentConfig {
  public static final String CONFIG_NAME = "content";
  private static final String ENABLED = "enabled";
  private static final String CONTENT_TYPE = "contentType";
  private Map<String, Object> mappedConfig;
  private final Config config;

  boolean enabled;
  String contentType;

  private ContentConfig(String configName) {
    config = Config.getInstance();
    mappedConfig = config.getJsonMapConfigNoCache(configName);
    setConfigData();
  }
  private ContentConfig() {
    this(CONFIG_NAME);
  }

  public static ContentConfig load(String configName) {
    return new ContentConfig(configName);
  }

  public static ContentConfig load() {
    return new ContentConfig();
  }

  public void reload() {
    mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
    setConfigData();
  }

  public void reload(String configName) {
    mappedConfig = config.getJsonMapConfigNoCache(configName);
    setConfigData();
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
