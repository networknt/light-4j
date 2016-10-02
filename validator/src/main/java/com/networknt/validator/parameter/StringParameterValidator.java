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

package com.networknt.validator.parameter;

import com.networknt.status.Status;
import io.swagger.models.parameters.SerializableParameter;

public class StringParameterValidator extends BaseParameterValidator {

    public StringParameterValidator() {
        super();
    }

    @Override
    public String supportedParameterType() {
        return "string";
    }

    @Override
    protected Status doValidate(
            final String value,
            final SerializableParameter parameter) {
        // TODO: Check pattern etc.
        return null;
    }
}
