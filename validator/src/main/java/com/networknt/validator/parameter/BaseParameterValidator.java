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
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.SerializableParameter;

abstract class BaseParameterValidator implements ParameterValidator {

    protected BaseParameterValidator() {

    }

    @Override
    public boolean supports(final Parameter p) {
        return p != null &&
                p instanceof SerializableParameter &&
                supportedParameterType().equalsIgnoreCase(((SerializableParameter)p).getType());
    }

    @Override
    public Status validate(final String value, final Parameter p) {

        if (!supports(p)) {
            return null;
        }

        final SerializableParameter parameter = (SerializableParameter)p;

        if (parameter.getRequired() && (value == null || value.trim().isEmpty())) {
            return new Status("ERR11001", p.getName());
        }

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        if (!matchesEnumIfDefined(value, parameter)) {
            return new Status("ERR11002", value, parameter.getName(), parameter.getEnum());
        }
        return doValidate(value, parameter);
    }

    private boolean matchesEnumIfDefined(final String value, final SerializableParameter parameter) {
        return parameter.getEnum() == null ||
                parameter.getEnum().isEmpty() ||
                parameter.getEnum().stream().anyMatch(value::equals);
    }

    /**
     * Perform type-specific validations
     *
     * @param value The value being validated
     * @param parameter The parameter the value is being validated against
     * @return Status The status object or null if there is no error
     */
    protected abstract Status doValidate(
            String value,
            SerializableParameter parameter);
}
