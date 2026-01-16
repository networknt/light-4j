package com.networknt.config.schema.generator;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.config.schema.*;

import javax.tools.FileObject;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Generates draft 07 JSON schema files for Light4J config POJOs.
 *
 * @author Kalev Gonvick
 */
public class JsonSchemaGenerator extends Generator {

    private static final String JSON_DRAFT = "http://json-schema.org/draft-07/schema#";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DefaultPrettyPrinter DEFAULT_PRETTY_PRINTER = new DefaultPrettyPrinter();
    private static final DefaultIndenter DEFAULT_INDENTER = new DefaultIndenter();

    /* Subset of OpenAPI specification fields used in the generator. */
    public static final String TYPE_KEY = "type";
    public static final String DESCRIPTION_KEY = "description";
    public static final String ADDITIONAL_PROPERTIES_KEY = "additionalProperties";
    public static final String MINIMUM_KEY = "minimum";
    public static final String MAXIMUM_KEY = "maximum";
    public static final String MIN_LENGTH_KEY = "minLength";
    public static final String MAX_LENGTH_KEY = "maxLength";
    public static final String PATTERN_KEY = "pattern";
    public static final String FORMAT_KEY = "format";
    public static final String ITEMS_KEY = "items";
    public static final String MIN_ITEMS_KEY = "minItems";
    public static final String MAX_ITEMS_KEY = "maxItems";
    public static final String UNIQUE_ITEMS_KEY = "uniqueItems";
    public static final String EXCLUSIVE_MIN_KEY = "exclusiveMin";
    public static final String EXCLUSIVE_MAX_KEY = "exclusiveMax";
    public static final String MULTIPLE_OF_KEY = "multipleOf";
    public static final String PROPERTIES_KEY = "properties";
    public static final String DEFAULT_KEY = "default";
    public static final String REQUIRED_KEY = "required";
    public static final String SCHEMA_KEY = "$schema";
    public static final String ALL_OF_KEY = "allOf";
    public static final String ANY_OF_KEY = "anyOf";
    public static final String ONE_OF_KEY = "oneOf";

    static {
        DEFAULT_INDENTER.withLinefeed("\n");
        DEFAULT_PRETTY_PRINTER.withObjectIndenter(DEFAULT_INDENTER);
        DEFAULT_PRETTY_PRINTER.withArrayIndenter(DEFAULT_INDENTER);
        OBJECT_MAPPER.setDefaultPrettyPrinter(DEFAULT_PRETTY_PRINTER);
    }

    public JsonSchemaGenerator(final String configKey, final String configName) {
        super(configKey, configName);
    }

    @Override
    public void writeSchemaToFile(OutputStream os, FieldNode annotatedField) throws IOException {
        final var schemaMap = this.addJsonSchemaRootInfo(annotatedField);
        OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(os, schemaMap);
    }

    @Override
    public void writeSchemaToFile(final FileObject source, final FieldNode annotatedField) throws IOException {
        final var schemaMap = this.addJsonSchemaRootInfo(annotatedField);
        OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(source.openOutputStream(), schemaMap);
    }

    @Override
    public void writeSchemaToFile(final Writer br, final FieldNode annotatedField) throws IOException {
        final var schemaMap = this.addJsonSchemaRootInfo(annotatedField);
        OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(br, schemaMap);
    }

    /**
     * Shared update logic when generating a JSON schema.
     *
     * @param annotatedField The metadata to generate the schema for.
     * @return The prepared JSON schema object.
     */
    protected LinkedHashMap<String, Object> addJsonSchemaRootInfo(final FieldNode annotatedField) {
        final var schemaMap = new LinkedHashMap<String, Object>();
        schemaMap.put(SCHEMA_KEY, JSON_DRAFT);
        schemaMap.put(TYPE_KEY, FieldType.OBJECT.toString());
        annotatedField.getOptionalChildNodes().ifPresent(nodes -> {
            List<String> required = nodes.stream()
                    .map(FieldNode::getConfigFieldName)
                    .collect(Collectors.toList());
            if (!required.isEmpty()) {
                schemaMap.put(REQUIRED_KEY, required);
                schemaMap.put(PROPERTIES_KEY, this.convertConfigRoot(annotatedField));
            }
        });
        return schemaMap;
    }

    @Override
    protected LinkedHashMap<String, Object> convertConfigRoot(final FieldNode annotatedField) {
        return annotatedField.getOptionalChildNodes()
                .filter(nodes -> !nodes.isEmpty())
                .map(nodes -> {
                    var props = new LinkedHashMap<String, Object>();
                    nodes.forEach(node -> props.put(node.getConfigFieldName(), this.convertNode(node)));
                    return props;
                })
                .or(() -> annotatedField.getOptionalRef().map(this::convertNode))
                .or(() -> annotatedField.getOptionalRefOneOf().map(fieldNodes -> {
                    var props = new LinkedHashMap<String, Object>();
                    var oneOfMembers = fieldNodes.stream()
                            .map(this::convertNode)
                            .collect(Collectors.toList());
                    props.put(ONE_OF_KEY, oneOfMembers);
                    return props;
                }))
                .or(() -> annotatedField.getOptionalRefAnyOf().map(fieldNodes -> {
                    var props = new LinkedHashMap<String, Object>();
                    var anyOfMembers = fieldNodes.stream()
                            .map(this::convertNode)
                            .collect(Collectors.toList());
                    props.put(ANY_OF_KEY, anyOfMembers);
                    return props;
                }))
                .or(() -> annotatedField.getOptionalRefAllOf().map(fieldNodes -> {
                    var props = new LinkedHashMap<String, Object>();
                    var allOfMembers = fieldNodes.stream()
                            .map(this::convertNode)
                            .collect(Collectors.toList());
                    props.put(ALL_OF_KEY, allOfMembers);
                    return props;
                }))
                .orElseGet(LinkedHashMap::new);
    }


    @Override
    protected LinkedHashMap<String, Object> convertStringNode(final FieldNode annotatedField) {
        var props = new LinkedHashMap<String, Object>();
        props.put(TYPE_KEY, annotatedField.getType().toString());
        annotatedField.getOptionalDescription().ifPresent(value -> props.put(DESCRIPTION_KEY, value));
        annotatedField.getOptionalDefaultValue().ifPresent(value -> props.put(DEFAULT_KEY, value));
        annotatedField.getOptionalMinLength().ifPresent(value -> props.put(MIN_LENGTH_KEY, value));
        annotatedField.getOptionalMaxLength().ifPresent(value -> props.put(MAX_LENGTH_KEY, value));
        annotatedField.getOptionalPattern().ifPresent(value -> props.put(PATTERN_KEY, value));
        annotatedField.getOptionalFormat().ifPresent(value -> props.put(FORMAT_KEY, value));
        return props;
    }

    @Override
    protected LinkedHashMap<String, Object> convertIntegerNode(final FieldNode annotatedField) {
        var props = new LinkedHashMap<String, Object>();
        props.put(TYPE_KEY, annotatedField.getType().toString());
        annotatedField.getOptionalDescription().ifPresent(description -> props.put(DESCRIPTION_KEY, description));
        annotatedField.getOptionalDefaultValue().ifPresent(value -> {
            try {
                props.put(DEFAULT_KEY, Integer.parseInt(value));
            } catch (Exception e) {
                // do nothing
            }
        });
        annotatedField.getOptionalMinInteger().ifPresent(value -> props.put(MINIMUM_KEY, value));
        annotatedField.getOptionalMaxInteger().ifPresent(value -> props.put(MAXIMUM_KEY, value));
        annotatedField.isOptionalExclusiveMin().ifPresent(value -> props.put(EXCLUSIVE_MIN_KEY, value));
        annotatedField.isOptionalExclusiveMax().ifPresent(value -> props.put(EXCLUSIVE_MAX_KEY, value));
        annotatedField.getOptionalMultipleOfInteger().ifPresent(value -> props.put(MULTIPLE_OF_KEY, value));
        annotatedField.getOptionalFormat().ifPresent(value -> props.put(FORMAT_KEY, value));
        return props;
    }

    @Override
    protected LinkedHashMap<String, Object> convertBooleanNode(final FieldNode annotatedField) {
        var props = new LinkedHashMap<String, Object>();
        props.put(TYPE_KEY, annotatedField.getType().toString());
        annotatedField.getOptionalDescription().ifPresent(value -> props.put(DESCRIPTION_KEY, value));
        annotatedField.getOptionalDefaultValue().ifPresent(value -> {
            if (value.equalsIgnoreCase("true")) {
                props.put(DEFAULT_KEY, true);
            } else if (value.equalsIgnoreCase("false")) {
                props.put(DEFAULT_KEY, false);
            }
        });
        return props;
    }

    @Override
    protected LinkedHashMap<String, Object> convertMapNode(final FieldNode annotatedField) {
        var props = new LinkedHashMap<String, Object>();
        props.put(TYPE_KEY, annotatedField.getType().toString());
        annotatedField.getOptionalDescription().ifPresent(value -> props.put(DESCRIPTION_KEY, value));

        var additionalProps = buildObjectProperties(annotatedField);
        props.put(ADDITIONAL_PROPERTIES_KEY, additionalProps);
        return props;
    }

    @Override
    protected LinkedHashMap<String, Object> convertNumberNode(final FieldNode annotatedField) {
        var props = new LinkedHashMap<String, Object>();
        props.put(TYPE_KEY, annotatedField.getType().toString());
        annotatedField.getOptionalDescription().ifPresent(value -> props.put(DESCRIPTION_KEY, value));
        annotatedField.getOptionalDefaultValue().ifPresent(value -> {
            try {
                props.put(DEFAULT_KEY, Double.parseDouble(value));
            } catch (Exception e) {
                // do nothing
            }
        });
        annotatedField.getOptionalMinNumber().ifPresent(value -> props.put(MINIMUM_KEY, value));
        annotatedField.getOptionalMaxNumber().ifPresent(value -> props.put(MAXIMUM_KEY, value));
        annotatedField.isOptionalExclusiveMin().ifPresent(value -> props.put(EXCLUSIVE_MIN_KEY, value));
        annotatedField.isOptionalExclusiveMax().ifPresent(value -> props.put(EXCLUSIVE_MAX_KEY, value));
        annotatedField.getOptionalMultipleOfNumber().ifPresent(value -> props.put(MULTIPLE_OF_KEY, value));
        annotatedField.getOptionalFormat().ifPresent(value -> props.put(FORMAT_KEY, value));
        return props;
    }


    @Override
    protected LinkedHashMap<String, Object> convertArrayNode(final FieldNode annotatedField) {
        var props = new LinkedHashMap<String, Object>();
        props.put(TYPE_KEY, annotatedField.getType().toString());
        annotatedField.getOptionalDescription().ifPresent(value -> props.put(DESCRIPTION_KEY, value));
        annotatedField.getOptionalMinItems().ifPresent(value -> props.put(MIN_ITEMS_KEY, value));
        annotatedField.getOptionalMaxItems().ifPresent(value -> props.put(MAX_ITEMS_KEY, value));
        annotatedField.isOptionalUnique().ifPresent(value -> props.put(UNIQUE_ITEMS_KEY, value));
        annotatedField.getOptionalDefaultValue().ifPresent(value -> {
            try {
                props.put(DEFAULT_KEY, OBJECT_MAPPER.readValue(value, Object.class));
            } catch (Exception e) {
                // No default value
            }
        });
        var items = buildObjectProperties(annotatedField);
        props.put(ITEMS_KEY, items);
        return props;
    }


    @Override
    protected LinkedHashMap<String, Object> convertNullNode(final FieldNode annotatedField) {
        var props = new LinkedHashMap<String, Object>();
        props.put(TYPE_KEY, annotatedField.getType().toString());
        annotatedField.getOptionalDefaultValue().ifPresent(value -> props.put(DEFAULT_KEY, value));
        return props;
    }


    @Override
    protected LinkedHashMap<String, Object> convertObjectNode(final FieldNode annotatedField) {
        var props = new LinkedHashMap<String, Object>();
        props.put(TYPE_KEY, annotatedField.getType().toString());
        annotatedField.getOptionalDescription().ifPresent(value -> props.put(DESCRIPTION_KEY, value));
        var hasProps = new AtomicBoolean(false);
        annotatedField.getOptionalRef().ifPresent(ref -> {
            hasProps.set(true);
            var refProps = this.convertNode(ref);
            props.putAll(refProps);
        });
        if (!hasProps.get())
            annotatedField.getOptionalRefOneOf().ifPresent(fieldNodes -> {
                hasProps.set(true);
                var oneOfMembers = new ArrayList<LinkedHashMap<String, Object>>();
                fieldNodes.stream().map(this::convertNode).forEach(oneOfMembers::add);
                props.put(ONE_OF_KEY, oneOfMembers);
            });
        if (!hasProps.get())
            annotatedField.getOptionalRefAnyOf().ifPresent(fieldNodes -> {
                hasProps.set(true);
                var anyOfMembers = new ArrayList<LinkedHashMap<String, Object>>();
                fieldNodes.stream().map(this::convertNode).forEach(anyOfMembers::add);
                props.put(ANY_OF_KEY, anyOfMembers);
            });

        if (!hasProps.get())
            annotatedField.getOptionalRefAllOf().ifPresent(fieldNodes -> {
                hasProps.set(true);
                var allOfMembers = new ArrayList<LinkedHashMap<String, Object>>();
                fieldNodes.stream().map(this::convertNode).forEach(allOfMembers::add);
                props.put(ALL_OF_KEY, allOfMembers);
            });

        if (!hasProps.get())
            annotatedField.getOptionalChildNodes().ifPresent(nodes -> {
                var innerProperties = new LinkedHashMap<String, Object>();
                nodes.stream()
                        .map(node -> new Object[]{node.getConfigFieldName(), this.convertNode(node)})
                        .forEach(tuple -> innerProperties.put((String) tuple[0], tuple[1]));
                props.put(PROPERTIES_KEY, innerProperties);
            });

        return props;
    }

    private LinkedHashMap<String, Object> buildObjectProperties(FieldNode annotatedField) {
        var props = new LinkedHashMap<String, Object>();
        annotatedField.getOptionalRefAllOf().ifPresent(nodes -> {
            var subProps = new ArrayList<LinkedHashMap<String, Object>>();
            nodes.stream().map(this::convertNode).forEach(subProps::add);
            props.put(ALL_OF_KEY, subProps);
        });

        if (props.isEmpty())
            annotatedField.getOptionalRefOneOf().ifPresent(nodes -> {
                var subProps = new ArrayList<LinkedHashMap<String, Object>>();
                nodes.stream().map(this::convertNode).forEach(subProps::add);
                props.put(ONE_OF_KEY, subProps);
            });

        if (props.isEmpty())
            annotatedField.getOptionalRefAnyOf().ifPresent(nodes -> {
                var subProps = new ArrayList<LinkedHashMap<String, Object>>();
                nodes.stream().map(this::convertNode).forEach(subProps::add);
                props.put(ANY_OF_KEY, subProps);
            });

        if (props.isEmpty()) {
            annotatedField.getOptionalRef().ifPresent(ref -> props.putAll(this.convertNode(ref)));
        }

        if (props.isEmpty())
            annotatedField.getOptionalChildNodes().ifPresent(nodes -> {
                var subProps = new LinkedHashMap<String, Object>();
                nodes.stream().map(this::convertNode).forEach(subProps::putAll);
                props.putAll(subProps);
            });
        return props;
    }
}
