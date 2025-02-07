package com.networknt.config.schema.generator;

import com.networknt.config.schema.MetadataParser;
import com.networknt.config.schema.OutputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * Base generator class for all schema types.
 *
 * @author Kalev Gonvick
 */
public abstract class Generator {

    private static final Logger LOG = LoggerFactory.getLogger(Generator.class);

    protected final String configKey;

    public Generator(final String configKey) {
        this.configKey = configKey;
    }

    public static Generator getGenerator(final OutputFormat format, final String configKey) {
        switch (format) {
            case JSON_SCHEMA:
                return new JsonSchemaGenerator(configKey);
            case YAML:
                return new YamlGenerator(configKey);
            case DEBUG:
                return new DebugGenerator(configKey);
            default:
                throw new IllegalArgumentException("Unsupported output format: " + format);
        }
    }

    protected <T> void updateIfNotDefault(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property, final String key, final Object defaultValue, final Class<T> type) {
        final var presentValue = this.getAsType(field.get(key), type);
        if (presentValue != null && !Objects.equals(presentValue, defaultValue)) {
            property.put(key, presentValue);
        }
    }

    /**
     * Writes the schema to a file.
     *
     * @param path     The path to write the schema to.
     * @param metadata The metadata to write.
     */
    public abstract void writeSchemaToFile(final String path, final LinkedHashMap<String, Object> metadata);

    /**
     * Parses an array field.
     * How the field gets parsed depends on the generator implementation.
     *
     * @param field    The field to parse.
     * @param property The property to update.
     */
    protected abstract void parseArray(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property);

    /**
     * Parses a boolean field.
     * How the field gets parsed depends on the generator implementation.
     *
     * @param field    The field to parse.
     * @param property The property to update.
     */
    protected abstract void parseBoolean(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property);

    /**
     * Parses an integer field.
     * How the field gets parsed depends on the generator implementation.
     *
     * @param field    The field to parse.
     * @param property The property to update.
     */
    protected abstract void parseInteger(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property);

    /**
     * Parses a number field.
     * How the field gets parsed depends on the generator implementation.
     *
     * @param field    The field to parse.
     * @param property The property to update.
     */
    protected abstract void parseNumber(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property);

    /**
     * Parses an object field.
     * How the field gets parsed depends on the generator implementation.
     *
     * @param field    The field to parse.
     * @param property The property to update.
     */
    protected abstract void parseObject(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property);

    /**
     * Parses a string field.
     * How the field gets parsed depends on the generator implementation.
     *
     * @param field    The field to parse.
     * @param property The property to update.
     */
    protected abstract void parseString(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property);

    /**
     * Parses a null field.
     * How the field gets parsed depends on the generator implementation.
     *
     * @param field    The field to parse.
     * @param property The property to update.
     */
    protected abstract void parseNullField(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property);

    /**
     * Gets the root schema properties.
     * How the field gets parsed depends on the generator implementation.
     *
     * @param metadata The metadata to get the properties from.
     * @return The root schema properties.
     */
    protected abstract LinkedHashMap<String, Object> getRootSchemaProperties(final LinkedHashMap<String, Object> metadata);

    /**
     * Parses a field.
     * How the field gets parsed depends on the generator implementation.
     *
     * @param field    The field to parse.
     * @param property The property to update.
     */
    protected void parseField(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property) {
        final var type = this.getAsType(field.get(MetadataParser.TYPE_KEY), String.class);
        switch (type) {
            case MetadataParser.ARRAY_TYPE:
                LOG.trace("Parsing array field: {}", field);
                this.parseArray(field, property);
                break;

            case MetadataParser.BOOLEAN_TYPE:
                LOG.trace("Parsing boolean field: {}", field);
                this.parseBoolean(field, property);
                break;

            case MetadataParser.INTEGER_TYPE:
                LOG.trace("Parsing integer field: {}", field);
                this.parseInteger(field, property);
                break;

            case MetadataParser.NUMBER_TYPE:
                LOG.trace("Parsing number field: {}", field);
                this.parseNumber(field, property);
                break;

            case MetadataParser.OBJECT_TYPE:
                LOG.trace("Parsing object field: {}", field);
                this.parseObject(field, property);
                break;

            case MetadataParser.STRING_TYPE:
                LOG.trace("Parsing string field: {}", field);
                this.parseString(field, property);
                break;

            case MetadataParser.NULL_TYPE:
                LOG.trace("Parsing null field: {}", field);
                this.parseNullField(field, property);
                break;

            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    /**
     * Casts the provided value to a specific class.
     *
     * @param value The value to cast.
     * @param type  The class to cast to.
     * @param <T>   The type to cast to.
     * @return The cast value.
     */
    protected <T> T getAsType(final Object value, Class<T> type) {
        if (value == null)
            return null;

        if (type.isInstance(value))
            return type.cast(value);

        else throw new IllegalArgumentException("Value is not of type " + type + ": " + value);
    }

}
