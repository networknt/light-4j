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


public class NumberParameterValidator extends BaseParameterValidator {

    public NumberParameterValidator() {
        super();
    }

    @Override
    public String supportedParameterType() {
        return "number";
    }

    @Override
    protected Status doValidate(final String value,
                                final SerializableParameter parameter) {
        if (parameter.getFormat().equalsIgnoreCase("float")) {
            try {
                Float.parseFloat(value);
            } catch (NumberFormatException e) {
                return failFormatValidation(value, parameter, "float");
            }
        } else if (parameter.getFormat().equalsIgnoreCase("double")){
            try {
                Double.parseDouble(value);
            } catch (NumberFormatException e) {
                return failFormatValidation(value, parameter, "double");
            }
        }

        final Double d = Double.parseDouble(value);
        if (parameter.getMinimum() != null && d < parameter.getMinimum()) {
            return new Status("ERR11011", value, parameter.getName(), parameter.getMinimum());
        }

        if (parameter.getMaximum() != null && d > parameter.getMaximum()) {
            return new Status("ERR11012", value, parameter.getName(), parameter.getMaximum());
        }
        return null;
    }

    private Status failFormatValidation(
            final String value,
            final SerializableParameter parameter,
            final String format) {
        return new Status("ERR11010", value, parameter.getName(), supportedParameterType(), format);
    }
}
