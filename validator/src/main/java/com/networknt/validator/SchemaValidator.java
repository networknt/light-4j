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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.config.Config;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;
import com.networknt.status.Status;
import com.networknt.utility.Util;
import io.swagger.models.Model;
import io.swagger.models.Swagger;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.StringProperty;
import io.swagger.util.Json;

import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Validate a value against the schema defined in a Swagger/OpenAPI specification.
 * <p>
 * Supports validation of properties and request/response bodies, and supports schema references.
 */
public class SchemaValidator {
    private static final String DEFINITIONS_FIELD = "definitions";
    static final String VALIDATOR_SCHEMA_INVALID_JSON = "ERR11003";
    static final String VALIDATOR_SCHEMA = "ERR11004";

    private final Swagger api;
    private JsonNode definitions;

    /**
     * Build a new validator with no API specification.
     * <p>
     * This will not perform any validation of $ref references that reference local definitions.
     *
     */
    public SchemaValidator() {
        this(null);
    }

    /**
     * Build a new validator for the given API specification.
     *
     * @param api The API to build the validator for. If provided, is used to retrieve schema definitions
     *            for use in references.
     */
    public SchemaValidator(final Swagger api) {
        this.api = api;
    }

    /**
     * Validate the given value against the given property schema.
     *
     * @param value The value to validate
     * @param schema The property schema to validate the value against
     *
     * @return A status containing error code and description
     */
    public Status validate(final Object value, final Property schema) {
        return doValidate(value, schema);
    }

    /**
     * Validate the given value against the given model schema.
     *
     * @param value The value to validate
     * @param schema The model schema to validate the value against
     *
     * @return A status containing error code and description
     */
    public Status validate(final Object value, final Model schema) {
        return doValidate(value, schema);
    }

    private Status doValidate(final Object value, final Object schema) {
        requireNonNull(schema, "A schema is required");

        Status status = null;
        Set<ValidationMessage> processingReport = null;
        try {
            final JsonNode schemaObject = Json.mapper().readTree(Json.pretty(schema));

            if (api != null) {
                if (this.definitions == null) {
                    this.definitions = Json.mapper().readTree(Json.pretty(api.getDefinitions()));
                }
                ((ObjectNode)schemaObject).set(DEFINITIONS_FIELD, this.definitions);
            }

            JsonSchema jsonSchema = new JsonSchemaFactory(Config.getInstance().getMapper()).getSchema(schemaObject);

            final JsonNode content = Json.mapper().valueToTree(value);
            processingReport = jsonSchema.validate(content);
        }
        catch (JsonParseException e) {
            return new Status(VALIDATOR_SCHEMA_INVALID_JSON, e.getMessage());
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if(processingReport != null && processingReport.size() > 0) {
            ValidationMessage vm = processingReport.iterator().next();
            status = new Status(VALIDATOR_SCHEMA, vm.getMessage());
        }

        return status;
    }

    /**
     * Validate the given value against the given property schema.
     *
     * @param value The value to validate
     * @param schema The property schema to validate the value against
     *
     * @return A status containing error code and description
     */
    public Status validate(final String value, final Property schema) {
        return doValidate(value, schema);
    }

    /**
     * Validate the given value against the given model schema.
     *
     * @param value The value to validate
     * @param schema The model schema to validate the value against
     *
     * @return A status containing error code and description
     */
    public Status validate(final String value, final Model schema) {
        return doValidate(value, schema);
    }

    private Status doValidate(final String value, final Object schema) {
        requireNonNull(schema, "A schema is required");

        Status status = null;
        Set<ValidationMessage> processingReport = null;
        try {
            final JsonNode schemaObject = Json.mapper().readTree(Json.pretty(schema));

            if (api != null) {
                if (this.definitions == null) {
                    this.definitions = Json.mapper().readTree(Json.pretty(api.getDefinitions()));
                }
                ((ObjectNode)schemaObject).set(DEFINITIONS_FIELD, this.definitions);
            }

            JsonSchema jsonSchema = new JsonSchemaFactory(Config.getInstance().getMapper()).getSchema(schemaObject);

            String normalisedValue = value;
            if (schema instanceof StringProperty) {
                normalisedValue = Util.quote(value);
            }

            final JsonNode content = Json.mapper().readTree(normalisedValue);
            processingReport = jsonSchema.validate(content);
        }
        catch (JsonParseException e) {
            return new Status(VALIDATOR_SCHEMA_INVALID_JSON, e.getMessage());
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if(processingReport != null && processingReport.size() > 0) {
            ValidationMessage vm = processingReport.iterator().next();
            status = new Status(VALIDATOR_SCHEMA, vm.getMessage());
        }

        return status;
    }
}
