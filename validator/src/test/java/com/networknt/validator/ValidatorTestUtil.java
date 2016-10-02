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

import io.swagger.models.parameters.SerializableParameter;
import io.swagger.models.properties.IntegerProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ValidatorTestUtil {

    private static final Logger log = LoggerFactory.getLogger(ValidatorTestUtil.class);


    /**
     * Load a response JSON file with the given name.
     *
     * @param responseName The name of the response to load
     *
     * @return The response JSON as a String, or <code>null</code> if it cannot be loaded
     */
    public static String loadResponse(final String responseName) {
        return loadResource("/responses/" + responseName + ".json");
    }

    /**
     * Load a request JSON file with the given name.
     *
     * @param requestName The name of the request to load
     *
     * @return The response JSON as a String, or <code>null</code> if it cannot be loaded
     */
    public static String loadRequest(final String requestName) {
        return loadResource("/requests/" + requestName + ".json");
    }

    private static String loadResource(final String path) {
        try {
            final InputStream stream = ValidatorTestUtil.class.getResourceAsStream(path);
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            final StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Int parameters

    public static SerializableParameter intParam() {
        return intParam(true, null, null);
    }

    public static SerializableParameter intParam(boolean required) {
        return intParam(required, null, null);
    }

    public static SerializableParameter intParam(final Double min, final Double max) {
        return intParam(true, min, max);
    }

    public static SerializableParameter intParam(final boolean required, final Double min, final Double max) {
        final SerializableParameter result = mock(SerializableParameter.class);
        when(result.getName()).thenReturn("Test Parameter");
        when(result.getType()).thenReturn("integer");
        when(result.getFormat()).thenReturn("int32");
        when(result.getRequired()).thenReturn(required);
        when(result.getMinimum()).thenReturn(min);
        when(result.getMaximum()).thenReturn(max);
        return result;
    }

    // String parameters

    public static SerializableParameter stringParam() {
        return stringParam(true);
    }

    public static SerializableParameter stringParam(final boolean required) {
        final SerializableParameter result = mock(SerializableParameter.class);
        when(result.getName()).thenReturn("Test Parameter");
        when(result.getType()).thenReturn("string");
        when(result.getRequired()).thenReturn(required);
        when(result.getMinimum()).thenReturn(null);
        when(result.getMaximum()).thenReturn(null);
        return result;
    }

    // Float parameters

    public static SerializableParameter floatParam() {
        return floatParam(true, null, null);
    }

    public static SerializableParameter floatParam(boolean required) {
        return floatParam(required, null, null);
    }

    public static SerializableParameter floatParam(final Double min, final Double max) {
        return floatParam(true, min, max);
    }

    public static SerializableParameter floatParam(final boolean required, final Double min, final Double max) {
        final SerializableParameter result = mock(SerializableParameter.class);
        when(result.getName()).thenReturn("Test Parameter");
        when(result.getType()).thenReturn("number");
        when(result.getFormat()).thenReturn("float");
        when(result.getRequired()).thenReturn(required);
        when(result.getMinimum()).thenReturn(min);
        when(result.getMaximum()).thenReturn(max);
        return result;
    }

    // Array parameters

    public static SerializableParameter intArrayParam(final boolean required,
                                                      final String collectionFormat) {
        final IntegerProperty property = new IntegerProperty();
        return arrayParam(required, collectionFormat, null, null, null, property);
    }

    public static SerializableParameter stringArrayParam(final boolean required,
                                                         final String collectionFormat) {
        final StringProperty property = new StringProperty();
        return arrayParam(required, collectionFormat, null, null, null, property);
    }

    public static SerializableParameter enumeratedArrayParam(final boolean required,
                                                             final String collectionFormat,
                                                             final Property items,
                                                             final String... enumValues) {
        final SerializableParameter result = mock(SerializableParameter.class);
        when(result.getName()).thenReturn("Test Parameter");
        when(result.getType()).thenReturn("array");
        when(result.getCollectionFormat()).thenReturn(collectionFormat);
        when(result.getRequired()).thenReturn(required);
        when(result.getMaxItems()).thenReturn(null);
        when(result.getMinItems()).thenReturn(null);
        when(result.getEnum()).thenReturn(asList(enumValues));
        when(result.getItems()).thenReturn(items);
        return result;
    }

    public static SerializableParameter arrayParam(final boolean required,
                                                   final String collectionFormat,
                                                   final Integer minItems,
                                                   final Integer maxItems,
                                                   final Boolean unique,
                                                   final Property items) {

        final SerializableParameter result = mock(SerializableParameter.class);
        when(result.getName()).thenReturn("Test Parameter");
        when(result.getType()).thenReturn("array");
        when(result.getCollectionFormat()).thenReturn(collectionFormat);
        when(result.getRequired()).thenReturn(required);
        when(result.getMinItems()).thenReturn(minItems);
        when(result.getMaxItems()).thenReturn(maxItems);
        when(result.isUniqueItems()).thenReturn(unique);
        when(result.getItems()).thenReturn(items);
        return result;
    }
}
