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

import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * A validator for array parameters.
 * <p>
 * This is a special-case validator as it needs to handle single and collection types for validation.
 */
public class ArrayParameterValidator extends BaseParameterValidator {

    public static final String ARRAY_PARAMETER_TYPE = "array";

    private final SchemaValidator schemaValidator;

    private enum CollectionFormat {
        CSV(","),
        SSV(" "),
        TSV("\t"),
        PIPES("\\|"),
        MULTI(null);

        final String separator;
        CollectionFormat(String separator) {
            this.separator = separator;
        }

        Collection<String> split(final String value) {
            if (separator == null) {
                return Collections.singleton(value);
            }
            return Arrays.asList(value.split(separator));
        }

        static CollectionFormat from(final SerializableParameter parameter) {
            requireNonNull(parameter, "A parameter is required");
            return valueOf(parameter.getCollectionFormat().toUpperCase());
        }
    }

    public ArrayParameterValidator(final SchemaValidator schemaValidator) {
        this.schemaValidator = schemaValidator == null ? new SchemaValidator() : schemaValidator;
    }

    @Override
    public String supportedParameterType() {
        return ARRAY_PARAMETER_TYPE;
    }

    @Override
    public Status validate(final String value, final Parameter p) {

        if (!supports(p)) {
            return null;
        }

        final SerializableParameter parameter = (SerializableParameter)p;

        if (parameter.getRequired() && (value == null || value.trim().isEmpty())) {
            return new Status("ERR11001", parameter.getName());
        }

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return doValidate(value, parameter);
    }

    public Status validate(final Collection<String> values, final Parameter p) {
        if (p == null) {
            return null;
        }

        final SerializableParameter parameter = (SerializableParameter)p;
        if (parameter.getRequired() && (values == null || values.isEmpty())) {
            return new Status("ERR11001", parameter.getName());
        }

        if (values == null) {
            return null;
        }

        if (!parameter.getCollectionFormat().equalsIgnoreCase(CollectionFormat.MULTI.name())) {
            return new Status("ERR11005", p.getName(), parameter.getCollectionFormat(), "multi");
        }

        return doValidate(values, parameter);
    }

    @Override
    protected Status doValidate(final String value, final SerializableParameter parameter) {
        return doValidate(CollectionFormat.from(parameter).split(value), parameter);
    }

    private Status doValidate(final Collection<String> values,
                            final SerializableParameter parameter) {

        if (parameter.getMaxItems() != null && values.size() > parameter.getMaxItems()) {
            return new Status("ERR11006", parameter.getName(), parameter.getMaxItems(), values.size());
        }

        if (parameter.getMinItems() != null && values.size() < parameter.getMinItems()) {
            return new Status("ERR11007", parameter.getName(), parameter.getMinItems(), values.size());
        }

        if (Boolean.TRUE.equals(parameter.isUniqueItems()) &&
                values.stream().distinct().count() != values.size()) {
            return new Status("ERR11008", parameter.getName());
        }

        if (parameter.getEnum() != null && !parameter.getEnum().isEmpty()) {
            final Set<String> enumValues = new HashSet<>(parameter.getEnum());
            Optional<String> value =
                values.stream()
                    .filter(v -> !enumValues.contains(v))
                    .findFirst();
            if(value.isPresent()) {
                return new Status("ERR11009", value.get(), parameter.getName(), parameter.getEnum());
            }
        }

        Optional<Status> optional =
                values.stream()
                .map(v -> schemaValidator.validate(v, parameter.getItems()))
                .filter(s -> s != null)
                .findFirst();
        if(optional.isPresent()) {
            return optional.get();
        } else {
            return null;
        }
    }
}
