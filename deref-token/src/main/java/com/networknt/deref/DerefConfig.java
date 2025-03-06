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

package com.networknt.deref;

import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.OutputFormat;

/**
 * The config class that maps to deref.yml
 *
 * @author Steve Hu
 */
@ConfigSchema(configKey = "dereg", configName = "deref", outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML})
public class DerefConfig {
    public static final String CONFIG_NAME = "deref";

    @BooleanField(
        configFieldName = "enabled",
        externalizedKeyName = "enabled",
        externalized = true,
        description = "indicate if the deref handler is enabled or not."
    )
    boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
