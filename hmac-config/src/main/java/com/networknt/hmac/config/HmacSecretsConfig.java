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
    @StringField(configFieldName = "selectorHeader", defaultValue = "")
    private String selectorHeader = "";

    // The schema generator cannot describe generic map values yet. The checked-in
    // schema narrows each value to an array of one or two strings.
    @MapField(configFieldName = "bySelector", additionalProperties = true, valueType = List.class)
    private Map<String, List<String>> bySelector = new LinkedHashMap<>();

    @ArrayField(configFieldName = "defaultEnvNames", items = String.class, defaultValue = "[]", maxItems = 2)
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
