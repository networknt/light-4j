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

package com.networknt.hmac.config;

import com.networknt.config.schema.ArrayField;
import com.networknt.config.schema.MapField;
import com.networknt.config.schema.StringField;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Environment-variable references used by one HMAC profile. */
public class HmacSecretsConfig {
    @StringField(configFieldName = "selectorHeader", defaultValue = "", description = "Optional; defaults to an empty string. Required when bySelector is non-empty. Selects a secret list using an exact header value after trimming HTTP whitespace.")
    private String selectorHeader = "";

    @MapField(configFieldName = "bySelector", additionalProperties = true, valueArray = @ArrayField(configFieldName = "selectorSecrets", items = String.class, minItems = 1, maxItems = 2, uniqueItems = true, itemsPattern = "[^\\s]" ), description = "Optional map; defaults to empty. Each selector maps to one or two distinct nonblank environment-variable names, current first and previous second. Quote numeric selector keys in YAML.")
    private Map<String, List<String>> bySelector = new LinkedHashMap<>();

    @ArrayField(configFieldName = "defaultEnvNames", items = String.class, defaultValue = "[]", maxItems = 2, uniqueItems = true, itemsPattern = "[^\\s]", description = "Optional; defaults to an empty list (no fallback). One or two distinct nonblank environment-variable names for a shared secret or an explicit fallback for missing/unknown selectors. At least this list or bySelector must be non-empty.")
    private List<String> defaultEnvNames = new ArrayList<>();

    public String getSelectorHeader() {
        return selectorHeader;
    }

    public void setSelectorHeader(String selectorHeader) {
        this.selectorHeader = selectorHeader;
    }

    public Map<String, List<String>> getBySelector() {
        return bySelector;
    }

    public void setBySelector(Map<String, List<String>> bySelector) {
        this.bySelector = bySelector;
    }

    public List<String> getDefaultEnvNames() {
        return defaultEnvNames;
    }

    public void setDefaultEnvNames(List<String> defaultEnvNames) {
        this.defaultEnvNames = defaultEnvNames;
    }
}
