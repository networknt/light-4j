package com.networknt.config.schema.generator;

import com.networknt.config.schema.*;

import javax.tools.FileObject;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.LinkedHashMap;

/**
 * Base generator class for all schema types.
 *
 * @author Kalev Gonvick
 */
public abstract class Generator {

    protected final String configKey;
    protected final String configName;

    protected Generator(final String configKey, final String configName) {
        this.configName = configName;
        this.configKey = configKey;
    }

    /**
     * Writes the schema to a file based on a provided FileObject.
     *
     * @param object         The file object to write the schema to.
     * @param annotatedField Contains all the data parsed from our annotated config field.
     * @throws IOException If an error occurs while writing the schema.
     */
    public abstract void writeSchemaToFile(final FileObject object, final FieldNode annotatedField) throws IOException;

    /**
     * Writes the schema to a file based on a provided Writer.
     *
     * @param writer         The writer to write the schema to.
     * @param annotatedField Contains all the data parsed from our annotated config field.
     * @throws IOException If an error occurs while writing the schema.
     */
    public abstract void writeSchemaToFile(final Writer writer, final FieldNode annotatedField) throws IOException;

    /**
     * Writes the schema to a file based on a provided OutputStream.
     *
     * @param os             The output stream to write the schema to.
     * @param annotatedField Contains all the data parsed from our annotated config field.
     */
    public abstract void writeSchemaToFile(final OutputStream os, final FieldNode annotatedField) throws IOException;

    /**
     * Converts an array field.
     * The input node is converted into the properties required for the generator.
     * How the field gets parsed depends on the generator implementation.
     *
     * @param annotatedField Contains all the data parsed from our annotated config field.
     * @return Converted array node entry.
     * @see FieldNode
     */
    protected abstract LinkedHashMap<String, Object> convertArrayNode(final FieldNode annotatedField);

    /**
     * Converts a boolean field.
     * The input node is converted into the properties required for the generator.
     * How the field gets parsed depends on the generator implementation.
     *
     * @param annotatedField Contains all the data parsed from our annotated config field.
     * @return Converted boolean node entry.
     * @see FieldNode
     */
    protected abstract LinkedHashMap<String, Object> convertBooleanNode(final FieldNode annotatedField);

    /**
     * Converts an integer field.
     * The input node is converted into the properties required for the generator.
     * How the field gets parsed depends on the generator implementation.
     *
     * @param annotatedField Contains all the data parsed from our annotated config field.
     * @return Converted integer node entry.
     * @see FieldNode
     */
    protected abstract LinkedHashMap<String, Object> convertIntegerNode(final FieldNode annotatedField);

    /**
     * Converts a number field.
     * The input node is converted into the properties required for the generator.
     * How the field gets parsed depends on the generator implementation.
     *
     * @param annotatedField Contains all the data parsed from our annotated config field.
     * @return Converted number node entry.
     * @see FieldNode
     */
    protected abstract LinkedHashMap<String, Object> convertNumberNode(final FieldNode annotatedField);

    /**
     * Parses an object field.
     * The input node is converted into the properties required for the generator.
     * How the field gets parsed depends on the generator implementation.
     *
     * @param annotatedField Contains all the data parsed from our annotated config field.
     * @return Converted object node entry.
     * @see FieldNode
     */
    protected abstract LinkedHashMap<String, Object> convertObjectNode(final FieldNode annotatedField);

    /**
     * Converts a string field.
     * The input node is converted into the properties required for the generator.
     * How the field gets parsed depends on the generator implementation.
     *
     * @param annotatedField Contains all the data parsed from our annotated config field.
     * @return Converted string node entry.
     * @see FieldNode
     */
    protected abstract LinkedHashMap<String, Object> convertStringNode(final FieldNode annotatedField);

    /**
     * Converts a null field.
     * The input node is converted into the properties required for the generator.
     * How the field gets parsed depends on the generator implementation.
     *
     * @param property The property to update.
     * @return Converted null node entry.
     * @see FieldNode
     */
    protected abstract LinkedHashMap<String, Object> convertNullNode(final FieldNode property);

    /**
     * Converts a map field.
     * The input node is converted into the properties required for the generator.
     * How the field gets parsed depends on the generator implementation.
     *
     * @param property Contains all the data parsed from our annotated config field.
     * @see FieldNode
     */
    protected abstract LinkedHashMap<String, Object> convertMapNode(final FieldNode property);

    /**
     * Converts the root {@link ConfigSchema} class into the root of the output format defined by the generator.
     *
     * @param annotatedField Contains all the data parsed from our annotated config field.
     * @return The root schema properties.
     * @see FieldNode
     */
    protected abstract LinkedHashMap<String, Object> convertConfigRoot(final FieldNode annotatedField);


    /**
     * Gets the generator for the specified output format.
     *
     * @param format    The output format to get the generator for.
     * @param configKey The configuration key to use.
     * @param configName The name of the configuration file.
     * @return The generator for the specified output format.
     *
     * @see OutputFormat
     */
    public static Generator getGenerator(final OutputFormat format, final String configKey, final String configName) {
        return format.getGenerator(configKey, configName);
    }

    /**
     * Converts a {@link FieldNode} into a hash entry.
     * How the node data gets ingested and the output structure are determined by the generator implementation.
     *
     * @param annotatedField The property to update.
     *
     * @see FieldNode
     */
    protected LinkedHashMap<String, Object> convertNode(final FieldNode annotatedField) {

        final var type = annotatedField.getType();
        switch (type) {
            case ARRAY:
                return this.convertArrayNode(annotatedField);
            case MAP:
                return this.convertMapNode(annotatedField);
            case BOOLEAN:
                return this.convertBooleanNode(annotatedField);
            case INTEGER:
                return this.convertIntegerNode(annotatedField);
            case NUMBER:
                return this.convertNumberNode(annotatedField);
            case OBJECT:
                return this.convertObjectNode(annotatedField);
            case STRING:
                return this.convertStringNode(annotatedField);
            case NULL:
                return this.convertNullNode(annotatedField);
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

}
