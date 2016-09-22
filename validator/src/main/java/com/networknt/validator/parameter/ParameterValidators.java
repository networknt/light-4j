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

import com.networknt.validator.SchemaValidator;
import com.networknt.validator.report.MessageResolver;
import com.networknt.validator.report.ValidationReport;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.SerializableParameter;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

public final class ParameterValidators {

    private final ArrayParameterValidator arrayValidator;
    private final MessageResolver messages;
    private final List<ParameterValidator> validators;

    /**
     * Create a new validators object with the given schema validator. If none is provided a default (empty) schema
     * validator will be used and no <code>ref</code> validation will be performed.
     *
     * @param schemaValidator The schema validator to use. If not provided a default (empty) validator will be used.
     * @param messages The message resolver to use.
     */
    public ParameterValidators(final SchemaValidator schemaValidator, MessageResolver messages) {
        this.arrayValidator = new ArrayParameterValidator(schemaValidator, messages);
        this.messages = requireNonNull(messages);
        this.validators = asList(
                new StringParameterValidator(messages),
                new NumberParameterValidator(messages),
                new IntegerParameterValidator(messages)
        );
    }

    public ValidationReport validate(final String value, final Parameter parameter) {
        requireNonNull(parameter);

        if ((parameter instanceof SerializableParameter) &&
                ((SerializableParameter)parameter).getType().equalsIgnoreCase("array")) {
            return arrayValidator.validate(value, parameter);
        }

        return validators.stream()
                .filter(v -> v.supports(parameter))
                .map(v -> v.validate(value, parameter))
                .reduce(ValidationReport.empty(), ValidationReport::merge);
    }
}
