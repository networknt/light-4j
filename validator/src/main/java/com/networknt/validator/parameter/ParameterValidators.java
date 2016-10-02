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
import com.networknt.validator.SchemaValidator;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.SerializableParameter;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

public final class ParameterValidators {

    private final ArrayParameterValidator arrayValidator;
    private final List<ParameterValidator> validators;

    /**
     * Create a new validators object with the given schema validator. If none is provided a default (empty) schema
     * validator will be used and no <code>ref</code> validation will be performed.
     *
     * @param schemaValidator The schema validator to use. If not provided a default (empty) validator will be used.
     */
    public ParameterValidators(final SchemaValidator schemaValidator) {
        this.arrayValidator = new ArrayParameterValidator(schemaValidator);
        this.validators = asList(
                new StringParameterValidator(),
                new NumberParameterValidator(),
                new IntegerParameterValidator()
        );
    }

    public Status validate(final String value, final Parameter parameter) {
        requireNonNull(parameter);

        if ((parameter instanceof SerializableParameter) &&
                ((SerializableParameter)parameter).getType().equalsIgnoreCase("array")) {
            return arrayValidator.validate(value, parameter);
        }


        Optional<Status> optional = validators.stream()
                .filter(v -> v.supports(parameter))
                .map(v -> v.validate(value, parameter))
                .filter(s -> s != null)
                .findFirst();
        if(optional.isPresent()) {
            return optional.get();
        } else {
            return null;
        }
    }

}
