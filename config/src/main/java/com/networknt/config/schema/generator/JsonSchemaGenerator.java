package com.networknt.config.schema.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.config.schema.AnnotationUtils;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.Format;
import com.networknt.config.schema.MetadataParser;

import javax.tools.FileObject;
import java.io.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * Generates draft 07 JSON schema files for Light4J config POJOs.
 *
 * @author Kalev Gonvick
 */
public class JsonSchemaGenerator extends Generator {

    private final static String JSON_DRAFT = "http://json-schema.org/draft-07/schema#";
    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final static DefaultPrettyPrinter DEFAULT_PRETTY_PRINTER = new DefaultPrettyPrinter();
    private final static DefaultIndenter DEFAULT_INDENTER = new DefaultIndenter();
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
    public void writeSchemaToFile(OutputStream os, LinkedHashMap<String, Object> metadata) throws IOException {
        final var schemaMap = this.prepJsonMetadataObject(metadata);
        OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(os, schemaMap);
    }

    @Override
    public void writeSchemaToFile(final FileObject source, final LinkedHashMap<String, Object> metadata) throws IOException {
        final var schemaMap = this.prepJsonMetadataObject(metadata);
        OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(source.openOutputStream(), schemaMap);
    }

    @Override
    public void writeSchemaToFile(final Writer br, final LinkedHashMap<String, Object> metadata) throws IOException {
        final var schemaMap = this.prepJsonMetadataObject(metadata);
        OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(br, schemaMap);
    }

    /**
     * Shared update logic when generating a JSON schema.
     * @param metadata The metadata to generate the schema for.
     * @return The prepared JSON schema object.
     */
    private LinkedHashMap<String, Object> prepJsonMetadataObject(final LinkedHashMap<String, Object> metadata) {

        final var schemaMap = new LinkedHashMap<String, Object>();
        schemaMap.put("$schema", JSON_DRAFT);
        schemaMap.put(MetadataParser.TYPE_KEY, MetadataParser.OBJECT_TYPE);

        if (Generator.fieldIsSubMap(metadata, MetadataParser.PROPERTIES_KEY)) {
            schemaMap.put("required", ((LinkedHashMap<String, Object>) metadata.get(MetadataParser.PROPERTIES_KEY)).keySet().toArray());
            schemaMap.put(MetadataParser.PROPERTIES_KEY, this.getRootSchemaProperties(metadata));

        } else schemaMap.put("additionalProperties", true);

        return schemaMap;
    }

    @Override
    protected LinkedHashMap<String, Object> getRootSchemaProperties(final LinkedHashMap<String, Object> metadata) {
        final var properties = new LinkedHashMap<String, Object>();
        final var metadataProperties = (LinkedHashMap<String, Object>) metadata.get(MetadataParser.PROPERTIES_KEY);

        metadataProperties.forEach((key, value) -> {
            final var property = new LinkedHashMap<String, Object>();
            this.parseField((LinkedHashMap<String, Object>) value, property);

            if (metadata.containsKey(MetadataParser.CONFIG_FIELD_NAME_KEY)
                    && !((String) metadata.get(MetadataParser.CONFIG_FIELD_NAME_KEY)).isEmpty())
                properties.put((String) metadata.get(MetadataParser.CONFIG_FIELD_NAME_KEY), property);

            else properties.put(key, property);
        });

        return properties;
    }


    @Override
    protected void parseString(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property) {
        property.put(MetadataParser.TYPE_KEY, MetadataParser.STRING_TYPE);
        AnnotationUtils.updateIfNotDefault(field, property, MetadataParser.DESCRIPTION_KEY, ConfigSchema.DEFAULT_STRING, String.class);

        /* special handling for default key in json schema */
        final var presentValue = AnnotationUtils.getAsType(field.get(MetadataParser.DEFAULT_VALUE_KEY), String.class);
        if (presentValue != null && !Objects.equals(presentValue, ConfigSchema.DEFAULT_STRING))
            property.put("default", presentValue);

        AnnotationUtils.updateIfNotDefault(field, property, MetadataParser.MIN_LENGTH_KEY, ConfigSchema.DEFAULT_INT, Integer.class);
        AnnotationUtils.updateIfNotDefault(field, property, MetadataParser.MAX_LENGTH_KEY, ConfigSchema.DEFAULT_MAX_INT, Integer.class);
        AnnotationUtils.updateIfNotDefault(field, property, MetadataParser.PATTERN_KEY, ConfigSchema.DEFAULT_STRING, String.class);
        AnnotationUtils.updateIfNotDefault(field, property, MetadataParser.FORMAT_KEY, Format.none.toString(), String.class);

    }

    @Override
    protected void parseInteger(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property) {
        property.put(MetadataParser.TYPE_KEY, MetadataParser.INTEGER_TYPE);
        AnnotationUtils.updateIfNotDefault(field, property, MetadataParser.DESCRIPTION_KEY, ConfigSchema.DEFAULT_STRING, String.class);

        /* special handling for default key in json schema */
        final var presentValue = AnnotationUtils.getAsType(field.get(MetadataParser.DEFAULT_VALUE_KEY), Integer.class);
        if (presentValue != null && !Objects.equals(presentValue, ConfigSchema.DEFAULT_INT))
            property.put("default", presentValue);

        AnnotationUtils.updateIfNotDefault(field, property, MetadataParser.MINIMUM_KEY, ConfigSchema.DEFAULT_MIN_INT, Integer.class);
        AnnotationUtils.updateIfNotDefault(field, property, MetadataParser.MAXIMUM_KEY, ConfigSchema.DEFAULT_MAX_INT, Integer.class);
        AnnotationUtils.updateIfNotDefault(field, property, MetadataParser.EXCLUSIVE_MIN_KEY, ConfigSchema.DEFAULT_BOOLEAN, Boolean.class);
        AnnotationUtils.updateIfNotDefault(field, property, MetadataParser.EXCLUSIVE_MAX_KEY, ConfigSchema.DEFAULT_BOOLEAN, Boolean.class);
        AnnotationUtils.updateIfNotDefault(field, property, MetadataParser.MULTIPLE_OF_KEY, ConfigSchema.DEFAULT_INT, Integer.class);
        AnnotationUtils.updateIfNotDefault(field, property, MetadataParser.FORMAT_KEY, Format.int32.toString(), String.class);

    }

    @Override
    protected void parseBoolean(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property) {
        property.put(MetadataParser.TYPE_KEY, MetadataParser.BOOLEAN_TYPE);
        AnnotationUtils.updateIfNotDefault(field, property, MetadataParser.DESCRIPTION_KEY, ConfigSchema.DEFAULT_STRING, String.class);

        final var presentValue = AnnotationUtils.getAsType(field.get(MetadataParser.DEFAULT_VALUE_KEY), Boolean.class);
        if (presentValue != null && !Objects.equals(presentValue, ConfigSchema.DEFAULT_BOOLEAN))
            property.put("default", presentValue);

    }

    @Override
    protected void parseMapField(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property) {
        property.put(MetadataParser.TYPE_KEY, MetadataParser.OBJECT_TYPE);
        AnnotationUtils.updateIfNotDefault(field, property, MetadataParser.DESCRIPTION_KEY, ConfigSchema.DEFAULT_STRING, String.class);
        final var valueProperties = new LinkedHashMap<String, Object>();

        assert Generator.fieldIsSubMap(field, MetadataParser.ADDITIONAL_PROPERTIES_KEY);
        final var valueMetadata = (LinkedHashMap<String, Object>) field.get(MetadataParser.ADDITIONAL_PROPERTIES_KEY);

        this.parseField(valueMetadata, valueProperties);

        property.put(MetadataParser.ADDITIONAL_PROPERTIES_KEY, valueProperties);
    }

    @Override
    protected void parseNumber(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property) {
        property.put(MetadataParser.TYPE_KEY, MetadataParser.NUMBER_TYPE);
        AnnotationUtils.updateIfNotDefault(field, property, MetadataParser.DESCRIPTION_KEY, ConfigSchema.DEFAULT_STRING, String.class);

        final var presentValue = AnnotationUtils.getAsType(field.get(MetadataParser.DEFAULT_VALUE_KEY), Number.class);
        if (presentValue != null && !Objects.equals(presentValue, ConfigSchema.DEFAULT_NUMBER))
            property.put("default", presentValue);

        AnnotationUtils.updateIfNotDefault(field, property, MetadataParser.MINIMUM_KEY, ConfigSchema.DEFAULT_MIN_NUMBER, Number.class);
        AnnotationUtils.updateIfNotDefault(field, property, MetadataParser.MAXIMUM_KEY, ConfigSchema.DEFAULT_MAX_NUMBER, Number.class);
        AnnotationUtils.updateIfNotDefault(field, property, MetadataParser.EXCLUSIVE_MIN_KEY, ConfigSchema.DEFAULT_BOOLEAN, Boolean.class);
        AnnotationUtils.updateIfNotDefault(field, property, MetadataParser.EXCLUSIVE_MAX_KEY, ConfigSchema.DEFAULT_BOOLEAN, Boolean.class);
        AnnotationUtils.updateIfNotDefault(field, property, MetadataParser.MULTIPLE_OF_KEY, ConfigSchema.DEFAULT_INT, Integer.class);
        AnnotationUtils.updateIfNotDefault(field, property, MetadataParser.FORMAT_KEY, Format.float32.toString(), String.class);
    }


    @Override
    protected void parseArray(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property) {
        property.put(MetadataParser.TYPE_KEY, MetadataParser.ARRAY_TYPE);
        AnnotationUtils.updateIfNotDefault(field, property, MetadataParser.DESCRIPTION_KEY, ConfigSchema.DEFAULT_STRING, String.class);
        AnnotationUtils.updateIfNotDefault(field, property, MetadataParser.MIN_ITEMS_KEY, ConfigSchema.DEFAULT_INT, Integer.class);
        AnnotationUtils.updateIfNotDefault(field, property, MetadataParser.MAX_ITEMS_KEY, ConfigSchema.DEFAULT_MAX_INT, Integer.class);
        AnnotationUtils.updateIfNotDefault(field, property, MetadataParser.UNIQUE_ITEMS_KEY, ConfigSchema.DEFAULT_BOOLEAN, Boolean.class);

        /* special handling for json default value */
        // TODO - handle useSubObjectDefault for array
        final var presentValue = AnnotationUtils.getAsType(field.get(MetadataParser.DEFAULT_VALUE_KEY), String.class);
        if (presentValue != null && !Objects.equals(presentValue, ConfigSchema.DEFAULT_STRING)) {
            try {
                property.put("default", this.OBJECT_MAPPER.readValue(presentValue, Object.class));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        final var arraySubProperties = new LinkedHashMap<String, Object>();
        final var items = (LinkedHashMap<String, Object>) field.get(MetadataParser.ITEMS_KEY);
        this.parseField(items, arraySubProperties);

        property.put(MetadataParser.ITEMS_KEY, arraySubProperties);
    }

    @Override
    protected void parseNullField(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property) {
        property.put(MetadataParser.TYPE_KEY, MetadataParser.NULL_TYPE);
        final var presentValue = AnnotationUtils.getAsType(field.get(MetadataParser.DEFAULT_VALUE_KEY), String.class);
        if (presentValue != null && !Objects.equals(presentValue, ConfigSchema.DEFAULT_STRING))
            property.put("default", presentValue);

    }

    @Override
    protected void parseObject(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property) {

        boolean usesAdditionalProperties = false;

        property.put(MetadataParser.TYPE_KEY, MetadataParser.OBJECT_TYPE);
        AnnotationUtils.updateIfNotDefault(field, property, MetadataParser.DESCRIPTION_KEY, ConfigSchema.DEFAULT_STRING, String.class);

        final LinkedHashMap<String, Object> objectProperties;
        if (Generator.fieldIsSubMap(field, MetadataParser.REF_KEY)) {
            objectProperties = (LinkedHashMap<String, Object>) ((LinkedHashMap<String, Object>) field.get(MetadataParser.REF_KEY)).get(MetadataParser.PROPERTIES_KEY);

        } else if (Generator.fieldIsSubMap(field, MetadataParser.ADDITIONAL_PROPERTIES_KEY)) {
            usesAdditionalProperties = true;
            objectProperties = (LinkedHashMap<String, Object>) ((LinkedHashMap<String, Object>) field.get(MetadataParser.ADDITIONAL_PROPERTIES_KEY)).get(MetadataParser.PROPERTIES_KEY);

        } else if (Generator.fieldIsSubMap(field, MetadataParser.PROPERTIES_KEY)) {
            objectProperties = (LinkedHashMap<String, Object>) field.get(MetadataParser.PROPERTIES_KEY);

        } else return; // No properties to parse, return early.


        /* special handling for json default value */
        final var presentValue = AnnotationUtils.getAsType(field.get(MetadataParser.DEFAULT_VALUE_KEY), String.class);
        if (presentValue != null && !Objects.equals(presentValue, ConfigSchema.DEFAULT_STRING)) {
            try {
                property.put("default", this.OBJECT_MAPPER.readValue(presentValue, Object.class));
            } catch (JsonProcessingException e) {
                // Do nothing, don't bother with the default value.
            }
        }

        final var subObjectProperties = new HashMap<String, Object>();
        objectProperties.forEach((key, value) -> {
            final var subProperty = new LinkedHashMap<String, Object>();
            this.parseField((LinkedHashMap<String, Object>) value, subProperty);

            if (subProperty.containsKey(MetadataParser.CONFIG_FIELD_NAME_KEY) && !((String) subProperty.get(MetadataParser.CONFIG_FIELD_NAME_KEY)).isEmpty())
                subObjectProperties.put((String) subProperty.get(MetadataParser.CONFIG_FIELD_NAME_KEY), subProperty);

            else subObjectProperties.put(key, subProperty);
        });

        if (usesAdditionalProperties) {
            final var wrapperObject = new LinkedHashMap<String, Object>();
            wrapperObject.put(MetadataParser.TYPE_KEY, MetadataParser.OBJECT_TYPE);
            wrapperObject.put(MetadataParser.PROPERTIES_KEY, subObjectProperties);
            property.put(MetadataParser.ADDITIONAL_PROPERTIES_KEY, wrapperObject);

        } else property.put(MetadataParser.PROPERTIES_KEY, subObjectProperties);

    }
}
