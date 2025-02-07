package com.networknt.config.schema.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.Format;
import com.networknt.config.schema.MetadataParser;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * Generates draft 07 JSON schema files.
 *
 * @author Kalev Gonvick
 */
public class JsonSchemaGenerator extends Generator {

    private final static String JSON_DRAFT = "http://json-schema.org/draft-07/schema#";
    private final ObjectMapper objectWriter = new ObjectMapper();

    public JsonSchemaGenerator(final String configKey) {
        super(configKey);
    }

    @Override
    public void writeSchemaToFile(final String path, final LinkedHashMap<String, Object> metadata) {
        LinkedHashMap<String, Object> schemaMap = new LinkedHashMap<>();
        schemaMap.put("$schema", JSON_DRAFT);

        schemaMap.put(MetadataParser.TYPE_KEY, MetadataParser.OBJECT_TYPE);

        if (metadata.get(MetadataParser.PROPERTIES_KEY) instanceof LinkedHashMap) {
            schemaMap.put("required", this.getRootSchemaRequiredFields((LinkedHashMap<String, Object>) metadata.get(MetadataParser.PROPERTIES_KEY)));
            schemaMap.put(MetadataParser.PROPERTIES_KEY, this.getRootSchemaProperties(metadata));

        } else schemaMap.put("additionalProperties", true);

        try {
            final var file = new File(path + "/" + configKey + ".json");
            this.objectWriter.writerWithDefaultPrettyPrinter().writeValue(file, schemaMap);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        this.updateIfNotDefault(field, property, MetadataParser.DESCRIPTION_KEY, ConfigSchema.DEFAULT_STRING, String.class);

        /* special handling for default key in json schema */
        final var presentValue = this.getAsType(field.get(MetadataParser.DEFAULT_VALUE_KEY), String.class);
        if (presentValue != null && !Objects.equals(presentValue, ConfigSchema.DEFAULT_STRING))
            property.put("default", presentValue);


        this.updateIfNotDefault(field, property, MetadataParser.MIN_LENGTH_KEY, ConfigSchema.DEFAULT_INT, Integer.class);
        this.updateIfNotDefault(field, property, MetadataParser.MAX_LENGTH_KEY, ConfigSchema.DEFAULT_MAX_INT, Integer.class);
        this.updateIfNotDefault(field, property, MetadataParser.PATTERN_KEY, ConfigSchema.DEFAULT_STRING, String.class);
        this.updateIfNotDefault(field, property, MetadataParser.FORMAT_KEY, Format.NONE.toString(), String.class);

    }

    @Override
    protected void parseInteger(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property) {
        property.put(MetadataParser.TYPE_KEY, MetadataParser.INTEGER_TYPE);
        this.updateIfNotDefault(field, property, MetadataParser.DESCRIPTION_KEY, ConfigSchema.DEFAULT_STRING, String.class);

        /* special handling for default key in json schema */
        final var presentValue = this.getAsType(field.get(MetadataParser.DEFAULT_VALUE_KEY), Integer.class);
        if (presentValue != null && !Objects.equals(presentValue, ConfigSchema.DEFAULT_INT))
            property.put("default", presentValue);

        this.updateIfNotDefault(field, property, MetadataParser.MINIMUM_KEY, ConfigSchema.DEFAULT_MIN_INT, Integer.class);
        this.updateIfNotDefault(field, property, MetadataParser.MAXIMUM_KEY, ConfigSchema.DEFAULT_MAX_INT, Integer.class);
        this.updateIfNotDefault(field, property, MetadataParser.EXCLUSIVE_MIN_KEY, ConfigSchema.DEFAULT_BOOLEAN, Boolean.class);
        this.updateIfNotDefault(field, property, MetadataParser.EXCLUSIVE_MAX_KEY, ConfigSchema.DEFAULT_BOOLEAN, Boolean.class);
        this.updateIfNotDefault(field, property, MetadataParser.MULTIPLE_OF_KEY, ConfigSchema.DEFAULT_INT, Integer.class);
        this.updateIfNotDefault(field, property, MetadataParser.FORMAT_KEY, Format.INT32.toString(), String.class);

    }

    @Override
    protected void parseBoolean(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property) {
        property.put(MetadataParser.TYPE_KEY, MetadataParser.BOOLEAN_TYPE);
        this.updateIfNotDefault(field, property, MetadataParser.DESCRIPTION_KEY, ConfigSchema.DEFAULT_STRING, String.class);

        final var presentValue = this.getAsType(field.get(MetadataParser.DEFAULT_VALUE_KEY), Boolean.class);
        if (presentValue != null && !Objects.equals(presentValue, ConfigSchema.DEFAULT_BOOLEAN))
            property.put("default", presentValue);

    }

    @Override
    protected void parseNumber(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property) {
        property.put(MetadataParser.TYPE_KEY, MetadataParser.NUMBER_TYPE);
        this.updateIfNotDefault(field, property, MetadataParser.DESCRIPTION_KEY, ConfigSchema.DEFAULT_STRING, String.class);

        final var presentValue = this.getAsType(field.get(MetadataParser.DEFAULT_VALUE_KEY), Number.class);
        if (presentValue != null && !Objects.equals(presentValue, ConfigSchema.DEFAULT_NUMBER))
            property.put("default", presentValue);

        this.updateIfNotDefault(field, property, MetadataParser.MINIMUM_KEY, ConfigSchema.DEFAULT_MIN_NUMBER, Number.class);
        this.updateIfNotDefault(field, property, MetadataParser.MAXIMUM_KEY, ConfigSchema.DEFAULT_MAX_NUMBER, Number.class);
        this.updateIfNotDefault(field, property, MetadataParser.EXCLUSIVE_MIN_KEY, ConfigSchema.DEFAULT_BOOLEAN, Boolean.class);
        this.updateIfNotDefault(field, property, MetadataParser.EXCLUSIVE_MAX_KEY, ConfigSchema.DEFAULT_BOOLEAN, Boolean.class);
        this.updateIfNotDefault(field, property, MetadataParser.MULTIPLE_OF_KEY, ConfigSchema.DEFAULT_INT, Integer.class);
        this.updateIfNotDefault(field, property, MetadataParser.FORMAT_KEY, Format.FLOAT32.toString(), String.class);

    }


    @Override
    protected void parseArray(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property) {
        property.put(MetadataParser.TYPE_KEY, MetadataParser.ARRAY_TYPE);
        this.updateIfNotDefault(field, property, MetadataParser.DESCRIPTION_KEY, ConfigSchema.DEFAULT_STRING, String.class);
        this.updateIfNotDefault(field, property, MetadataParser.MIN_ITEMS_KEY, ConfigSchema.DEFAULT_INT, Integer.class);
        this.updateIfNotDefault(field, property, MetadataParser.MAX_ITEMS_KEY, ConfigSchema.DEFAULT_MAX_INT, Integer.class);
        this.updateIfNotDefault(field, property, MetadataParser.UNIQUE_ITEMS_KEY, ConfigSchema.DEFAULT_BOOLEAN, Boolean.class);

        /* special handling for json default value */
        // TODO - handle useSubObjectDefault for array
        final var presentValue = this.getAsType(field.get(MetadataParser.DEFAULT_VALUE_KEY), String.class);
        if (presentValue != null && !Objects.equals(presentValue, ConfigSchema.DEFAULT_STRING)) {
            try {
                property.put("default", this.objectWriter.readValue(presentValue, Object.class));
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
        final var presentValue = this.getAsType(field.get(MetadataParser.DEFAULT_VALUE_KEY), String.class);
        if (presentValue != null && !Objects.equals(presentValue, ConfigSchema.DEFAULT_STRING)) {
            property.put("default", presentValue);
        }
    }

    @Override
    protected void parseObject(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property) {
        property.put(MetadataParser.TYPE_KEY, MetadataParser.OBJECT_TYPE);
        this.updateIfNotDefault(field, property, MetadataParser.DESCRIPTION_KEY, ConfigSchema.DEFAULT_STRING, String.class);
        final var subObjectProperties = new HashMap<String, Object>();
        final var objectProperties = (LinkedHashMap<String, Object>) field.get(MetadataParser.PROPERTIES_KEY);

        /* special handling for json default value */
        // TODO - handle useSubObjectDefault for object
        final var presentValue = this.getAsType(field.get(MetadataParser.DEFAULT_VALUE_KEY), String.class);
        if (presentValue != null && !Objects.equals(presentValue, ConfigSchema.DEFAULT_STRING)) {
            try {
                property.put("default", this.objectWriter.readValue(presentValue, Object.class));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        objectProperties.forEach((key, value) -> {
            final var subProperty = new LinkedHashMap<String, Object>();
            this.parseField((LinkedHashMap<String, Object>) value, subProperty);

            if (field.containsKey(MetadataParser.CONFIG_FIELD_NAME_KEY)
                    && !((String) field.get(MetadataParser.CONFIG_FIELD_NAME_KEY)).isEmpty())
                subObjectProperties.put((String) field.get(MetadataParser.CONFIG_FIELD_NAME_KEY), subProperty);

            else subObjectProperties.put(key, subProperty);
        });
        property.put(MetadataParser.PROPERTIES_KEY, subObjectProperties);
    }

    private String[] getRootSchemaRequiredFields(final LinkedHashMap<String, Object> metadata) {
        final var fields = metadata.keySet().toArray();
        final var required = new String[fields.length];

        for (int index = 0; index < fields.length; index++)
            required[index] = fields[index].toString();

        return required;
    }
}
