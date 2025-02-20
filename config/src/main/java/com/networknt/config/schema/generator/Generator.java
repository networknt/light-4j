package com.networknt.config.schema.generator;

import com.networknt.config.schema.MetadataParser;
import com.networknt.config.schema.OutputFormat;

import javax.tools.FileObject;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * Base generator class for all schema types.
 *
 * @author Kalev Gonvick
 */
public abstract class Generator {

    protected final String configKey;
    protected final String configName;

    public Generator(final String configKey, final String configName) {
        this.configName = configName;
        this.configKey = configKey;
    }

    /**
     * Writes the schema to a file based on a provided FileObject.
     *
     * @param object   The file object to write the schema to.
     * @param metadata The metadata to write.
     * @throws IOException If an error occurs while writing the schema.
     */
    public abstract void writeSchemaToFile(final FileObject object, final LinkedHashMap<String, Object> metadata) throws IOException;

    /**
     * Writes the schema to a file based on a provided Writer.
     *
     * @param writer   The writer to write the schema to.
     * @param metadata The metadata to write.
     * @throws IOException If an error occurs while writing the schema.
     */
    public abstract void writeSchemaToFile(final Writer writer, final LinkedHashMap<String, Object> metadata) throws IOException;

    /**
     * Writes the schema to a file based on a provided OutputStream.
     *
     * @param os       The output stream to write the schema to.
     * @param metadata The metadata to write.
     */
    public abstract void writeSchemaToFile(final OutputStream os, final LinkedHashMap<String, Object> metadata) throws IOException;

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
     * Parses a map field.
     * How the field gets parsed depends on the generator implementation.
     *
     * @param field    The field to parse.
     * @param property The property to update.
     */
    protected abstract void parseMapField(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property);

    /**
     * Gets the root schema properties.
     * How the field gets parsed depends on the generator implementation.
     *
     * @param metadata The metadata to get the properties from.
     * @return The root schema properties.
     */
    protected abstract LinkedHashMap<String, Object> getRootSchemaProperties(final LinkedHashMap<String, Object> metadata);


    /**
     * Gets the generator for the specified output format.
     *
     * @param format    The output format to get the generator for.
     * @param configKey The configuration key to use.
     * @return The generator for the specified output format.
     */
    public static Generator getGenerator(final OutputFormat format, final String configKey, final String configName) {
        switch (format) {
            case JSON_SCHEMA:
                return new JsonSchemaGenerator(configKey, configName);
            case YAML:
                return new YamlGenerator(configKey, configName);
            case DEBUG:
                return new DebugGenerator(configKey, configName);
            default:
                throw new IllegalArgumentException("Unsupported output format: " + format);
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
    protected static <T> T getAsType(final Object value, Class<T> type) {
        if (value == null)
            return null;

        if (type.isInstance(value))
            return type.cast(value);

        else throw new IllegalArgumentException("Value is not of type " + type + ": " + value);
    }

    /**
     * Updates the property if the field is not the default value.
     *
     * @param field        The field to update.
     * @param property     The property to update.
     * @param key          The key to update.
     * @param defaultValue The default value.
     * @param type         The class type of the value.
     * @param <T>          The type of the value.
     */
    protected static <T> void updateIfNotDefault(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property, final String key, final Object defaultValue, final Class<T> type) {
        final var value = getAsType(field.get(key), type);
        if (value != null && !Objects.equals(value, defaultValue))
            property.put(key, value);

    }

    /**
     * Common check to see if the current hashmap contains another hashmap at a specific field.
     *
     * @param map   The map to check.
     * @param field The field to check.
     * @return True if the map contains the field and the field is a hashmap.
     */
    protected static boolean fieldIsSubMap(final LinkedHashMap<String, Object> map, final String field) {
        return map.containsKey(field) && map.get(field) instanceof LinkedHashMap;
    }

    /**
     * Parses a field.
     * How the field gets parsed depends on the generator implementation.
     *
     * @param field    The field to parse.
     * @param property The property to update.
     */
    protected void parseField(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property) {
        final var type = getAsType(field.get(MetadataParser.TYPE_KEY), String.class);
        switch (type) {
            case MetadataParser.ARRAY_TYPE:
                this.parseArray(field, property);
                break;

            case MetadataParser.MAP_TYPE:
                this.parseMapField(field, property);
                break;

            case MetadataParser.BOOLEAN_TYPE:
                this.parseBoolean(field, property);
                break;

            case MetadataParser.INTEGER_TYPE:
                this.parseInteger(field, property);
                break;

            case MetadataParser.NUMBER_TYPE:
                this.parseNumber(field, property);
                break;

            case MetadataParser.OBJECT_TYPE:
                this.parseObject(field, property);
                break;

            case MetadataParser.STRING_TYPE:
                this.parseString(field, property);
                break;

            case MetadataParser.NULL_TYPE:
                this.parseNullField(field, property);
                break;

            default:
                throw new IllegalArgumentException("Unsupported type: " + type + " - Metadata: " + field);
        }
    }

}
