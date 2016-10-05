/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
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

package com.networknt.validator;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by steve on 17/09/16.
 */
public class ValidatorConfig {
    boolean enabled;
    boolean enableResponseValidator;

    @JsonIgnore
    String description;

    public ValidatorConfig() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnableResponseValidator() {
        return enableResponseValidator;
    }

    public void setEnableResponseValidator(boolean enableResponseValidator) {
        this.enableResponseValidator = enableResponseValidator;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
