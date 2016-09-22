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
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.config.Config;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;
import com.networknt.utility.Util;
import com.networknt.validator.report.MessageResolver;
import com.networknt.validator.report.MutableValidationReport;
import com.networknt.validator.report.ValidationReport;
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
    private static final String ADDITIONAL_PROPERTIES_FIELD = "additionalProperties";
    private static final String DEFINITIONS_FIELD = "definitions";

    private final Swagger api;
    private JsonNode definitions;
    private final MessageResolver messages;

    /**
     * Build a new validator with no API specification.
     * <p>
     * This will not perform any validation of $ref references that reference local definitions.
     *
     * @param messages The message resolver to use
     */
    public SchemaValidator(final MessageResolver messages) {
        this(null, messages);
    }

    /**
     * Build a new validator for the given API specification.
     *
     * @param api The API to build the validator for. If provided, is used to retrieve schema definitions
     *            for use in references.
     * @param messages The message resolver to use.
     */
    public SchemaValidator(final Swagger api, final MessageResolver messages) {
        this.api = api;
        this.messages = requireNonNull(messages, "A message resolver is required");
    }

    /**
     * Validate the given value against the given property schema.
     *
     * @param value The value to validate
     * @param schema The property schema to validate the value against
     *
     * @return A validation report containing accumulated validation errors
     */
    public ValidationReport validate(final String value, final Property schema) {
        return doValidate(value, schema);
    }

    /**
     * Validate the given value against the given model schema.
     *
     * @param value The value to validate
     * @param schema The model schema to validate the value against
     *
     * @return A validation report containing accumulated validation errors
     */
    public ValidationReport validate(final String value, final Model schema) {
        return doValidate(value, schema);
    }

    private ValidationReport doValidate(final String value, final Object schema) {
        requireNonNull(schema, "A schema is required");

        final MutableValidationReport validationReport = new MutableValidationReport();
        Set<ValidationMessage> processingReport = null;
        try {
            final JsonNode schemaObject = Json.mapper().readTree(Json.pretty(schema));

            /*
            if (schemaObject instanceof ObjectNode) {
                ((ObjectNode)schemaObject).set(ADDITIONAL_PROPERTIES_FIELD, BooleanNode.getFalse());
            }
            */

            if (api != null) {
                if (this.definitions == null) {
                    this.definitions = Json.mapper().readTree(Json.pretty(api.getDefinitions()));

                    // Explicitly disable additionalProperties
                    // Calling code can choose what level to emit this failure at using validation.schema.additionalProperties
                    /*
                    this.definitions.forEach(n -> {
                        if (!n.has(ADDITIONAL_PROPERTIES_FIELD)) {
                            ((ObjectNode)n).set(ADDITIONAL_PROPERTIES_FIELD, BooleanNode.getFalse());
                        }
                    });
                    */
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
            validationReport.add(messages.get("validation.schema.invalidJson", e.getMessage()));
            return validationReport;
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if(processingReport != null && processingReport.size() > 0) {
            processingReport.forEach(vm -> {
                final String type = vm.getType();
                validationReport.add(messages.create("validation.schema." + type, vm.getMessage()));
            });
        }

        return validationReport;
    }
}
