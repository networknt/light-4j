package com.networknt.config.schema.generator;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.config.schema.*;

import javax.tools.FileObject;
import java.io.*;
import java.util.*;
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
        DefaultIndenter lfIndenter = new DefaultIndenter().withLinefeed("\n");
        DEFAULT_PRETTY_PRINTER.withObjectIndenter(lfIndenter);
        DEFAULT_PRETTY_PRINTER.withArrayIndenter(lfIndenter);
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


    @Override
    protected LinkedHashMap<String, Object> convertConfigRoot(final FieldNode annotatedField) {
        var props = new LinkedHashMap<String, Object>();
        annotatedField.getAllOf().map(nodes -> this.buildMultiClassJsonProperties(ALL_OF_KEY, nodes))
                .or(() -> annotatedField.getAnyOf().map(nodes -> this.buildMultiClassJsonProperties(ANY_OF_KEY, nodes)))
                .or(() -> annotatedField.getOneOf().map(nodes -> this.buildMultiClassJsonProperties(ONE_OF_KEY, nodes)))
                .or(() -> annotatedField.getRef().map(this::convertNode))
                .or(() -> annotatedField.getChildren().map(nodes -> {
                    var inner = new LinkedHashMap<String, Object>();
                    nodes.forEach(node -> inner.put(node.getConfigFieldName(), this.convertNode(node)));
                    return inner;
                }))
                .ifPresent(props::putAll);
        return props;
    }


    @Override
    protected LinkedHashMap<String, Object> convertStringNode(final FieldNode annotatedField) {
        var props = new LinkedHashMap<String, Object>();
        props.put(TYPE_KEY, annotatedField.getType().toString());
        annotatedField.getDescription().ifPresent(value -> props.put(DESCRIPTION_KEY, value));
        annotatedField.getDefaultValue().ifPresent(value -> props.put(DEFAULT_KEY, value));
        annotatedField.getMinLength().ifPresent(value -> props.put(MIN_LENGTH_KEY, value));
        annotatedField.getMaxLength().ifPresent(value -> props.put(MAX_LENGTH_KEY, value));
        annotatedField.getPattern().ifPresent(value -> props.put(PATTERN_KEY, value));
        annotatedField.getFormat().ifPresent(value -> props.put(FORMAT_KEY, value));
        return props;
    }

    @Override
    protected LinkedHashMap<String, Object> convertIntegerNode(final FieldNode annotatedField) {
        var props = new LinkedHashMap<String, Object>();
        props.put(TYPE_KEY, annotatedField.getType().toString());
        annotatedField.getDescription().ifPresent(description -> props.put(DESCRIPTION_KEY, description));
        annotatedField.getDefaultValue().ifPresent(value -> {
            try {
                props.put(DEFAULT_KEY, Integer.parseInt(value));
            } catch (Exception e) {
                // do nothing
            }
        });
        annotatedField.getMinInteger().ifPresent(value -> props.put(MINIMUM_KEY, value));
        annotatedField.getMaxInteger().ifPresent(value -> props.put(MAXIMUM_KEY, value));
        annotatedField.getExclusiveMax().ifPresent(value -> props.put(EXCLUSIVE_MIN_KEY, value));
        annotatedField.getExclusiveMin().ifPresent(value -> props.put(EXCLUSIVE_MAX_KEY, value));
        annotatedField.getMultipleOfInteger().ifPresent(value -> props.put(MULTIPLE_OF_KEY, value));
        annotatedField.getFormat().ifPresent(value -> props.put(FORMAT_KEY, value));
        return props;
    }

    @Override
    protected LinkedHashMap<String, Object> convertBooleanNode(final FieldNode annotatedField) {
        var props = new LinkedHashMap<String, Object>();
        props.put(TYPE_KEY, annotatedField.getType().toString());
        annotatedField.getDescription().ifPresent(value -> props.put(DESCRIPTION_KEY, value));
        annotatedField.getDefaultValue().ifPresent(value -> {
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
        annotatedField.getDescription().ifPresent(value -> props.put(DESCRIPTION_KEY, value));

        var additionalProps = buildNestedJsonProperties(annotatedField);
        props.put(ADDITIONAL_PROPERTIES_KEY, additionalProps);
        return props;
    }

    @Override
    protected LinkedHashMap<String, Object> convertNumberNode(final FieldNode annotatedField) {
        var props = new LinkedHashMap<String, Object>();
        props.put(TYPE_KEY, annotatedField.getType().toString());
        annotatedField.getDescription().ifPresent(value -> props.put(DESCRIPTION_KEY, value));
        annotatedField.getDefaultValue().ifPresent(value -> {
            try {
                props.put(DEFAULT_KEY, Double.parseDouble(value));
            } catch (Exception e) {
                // do nothing
            }
        });
        annotatedField.getMinNumber().ifPresent(value -> props.put(MINIMUM_KEY, value));
        annotatedField.getMaxNumber().ifPresent(value -> props.put(MAXIMUM_KEY, value));
        annotatedField.getExclusiveMax().ifPresent(value -> props.put(EXCLUSIVE_MIN_KEY, value));
        annotatedField.getExclusiveMin().ifPresent(value -> props.put(EXCLUSIVE_MAX_KEY, value));
        annotatedField.getMultipleOfNumber().ifPresent(value -> props.put(MULTIPLE_OF_KEY, value));
        annotatedField.getFormat().ifPresent(value -> props.put(FORMAT_KEY, value));
        return props;
    }


    @Override
    protected LinkedHashMap<String, Object> convertArrayNode(final FieldNode annotatedField) {
        var props = new LinkedHashMap<String, Object>();
        props.put(TYPE_KEY, annotatedField.getType().toString());
        annotatedField.getDescription().ifPresent(value -> props.put(DESCRIPTION_KEY, value));
        annotatedField.getMinItems().ifPresent(value -> props.put(MIN_ITEMS_KEY, value));
        annotatedField.getMaxItems().ifPresent(value -> props.put(MAX_ITEMS_KEY, value));
        annotatedField.getUnique().ifPresent(value -> props.put(UNIQUE_ITEMS_KEY, value));
        annotatedField.getDefaultValue().ifPresent(value -> {
            try {
                props.put(DEFAULT_KEY, OBJECT_MAPPER.readValue(value, Object.class));
            } catch (Exception e) {
                // No default value
            }
        });
        var items = buildNestedJsonProperties(annotatedField);
        props.put(ITEMS_KEY, items);
        return props;
    }


    @Override
    protected LinkedHashMap<String, Object> convertNullNode(final FieldNode annotatedField) {
        var props = new LinkedHashMap<String, Object>();
        props.put(TYPE_KEY, annotatedField.getType().toString());
        annotatedField.getDefaultValue().ifPresent(value -> props.put(DEFAULT_KEY, value));
        return props;
    }


    @Override
    protected LinkedHashMap<String, Object> convertObjectNode(final FieldNode annotatedField) {
        var props = new LinkedHashMap<String, Object>();
        props.put(TYPE_KEY, annotatedField.getType().toString());
        annotatedField.getDescription().ifPresent(value -> props.put(DESCRIPTION_KEY, value));
        annotatedField.getDefaultValue().ifPresent(value -> {
            try {
                props.put(DEFAULT_KEY, OBJECT_MAPPER.readValue(value, Object.class));
            } catch (Exception e) {
                // No default value
            }
        });
        annotatedField.getAllOf().map(nodes -> this.buildMultiClassJsonProperties(ALL_OF_KEY, nodes))
                .or(() -> annotatedField.getAnyOf().map(nodes -> this.buildMultiClassJsonProperties(ANY_OF_KEY, nodes)))
                .or(() -> annotatedField.getOneOf().map(nodes -> this.buildMultiClassJsonProperties(ONE_OF_KEY, nodes)))
                .or(() -> annotatedField.getRef().map(this::convertNode))
                .or(() -> annotatedField.getChildren().map(nodes -> {
                    var outer = new LinkedHashMap<String, Object>();
                    var inner = new LinkedHashMap<String, Object>();
                    nodes.stream()
                            .map(node -> new Object[]{
                                    node.getConfigFieldName(),
                                    this.convertNode(node)
                            })
                            .forEach(tuple -> inner.put((String) tuple[0], tuple[1]));
                    outer.put(PROPERTIES_KEY, inner);
                    return outer;
                }))
                .ifPresent(props::putAll);
        return props;
    }

    private LinkedHashMap<String, Object> buildNestedJsonProperties(final FieldNode annotatedField) {
        return annotatedField.getAllOf().map(nodes -> this.buildMultiClassJsonProperties(ALL_OF_KEY, nodes))
                .or(() -> annotatedField.getOneOf().map(nodes -> this.buildMultiClassJsonProperties(ONE_OF_KEY, nodes)))
                .or(() -> annotatedField.getAnyOf().map(nodes -> this.buildMultiClassJsonProperties(ANY_OF_KEY, nodes)))
                .or(() -> annotatedField.getRef().map(this::convertNode))
                .or(() -> annotatedField.getChildren().map(nodes -> {
                    var outer = new LinkedHashMap<String, Object>();
                    nodes.stream().map(this::convertNode).forEach(outer::putAll);
                    return outer;
                }))
                .orElse(new LinkedHashMap<>());
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
        annotatedField.getChildren().ifPresent(nodes -> {
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


    /**
     * Builds multi-class json properties. i.e. anyOf, allOf, oneOf, etc.
     *
     * @param key - The keyword to identify the multi-class type.
     * @param nodes - The list of nodes.
     * @return - Returns a list of converted classes.
     */
    private LinkedHashMap<String, Object> buildMultiClassJsonProperties(final String key, final List<FieldNode> nodes) {
        var outer = new LinkedHashMap<String, Object>();
        var inner = new ArrayList<LinkedHashMap<String, Object>>();
        nodes.stream().map(this::convertNode).forEach(inner::add);
        outer.put(key, inner);
        return outer;
    }
}
