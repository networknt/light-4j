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

package com.networknt.exception;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.OutputFormat;

/**
 * Config class for Exception module to control the behavior
 *
 * @author  Steve Hu
 */
@ConfigSchema(
        configName = "exception",
        configKey = "exception",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD},
        configDescription = "Exception handler for runtime exception and ApiException if it is not handled by other handlers in the chain."
        )
public class ExceptionConfig {
    public static final String CONFIG_NAME = "exception";

    @BooleanField(
            configFieldName = "enabled",
            externalizedKeyName = "enabled",
            defaultValue = "true",
            externalized = true,
            description = "Enable or disable the exception module."
    )
    boolean enabled;

    @JsonIgnore
    String description;

    public ExceptionConfig() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
