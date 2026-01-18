package com.networknt.config.schema.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.config.schema.FieldNode;

import javax.tools.FileObject;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.LinkedHashMap;

/**
 * A generator that writes the metadata to a file in a human-readable format.
 * Used for debugging the config generator.
 *
 * @author Kalev Gonvick
 */
public class DebugGenerator extends Generator {

    private final ObjectMapper objectWriter = new ObjectMapper();

    public DebugGenerator(final String configKey, final String configName) {
        super(configKey, configName);
    }

    @Override
    public void writeSchemaToFile(final FileObject path, FieldNode annotatedField) throws IOException {
        writeSchemaToFile(path.openOutputStream(), annotatedField);
    }

    @Override
    public void writeSchemaToFile(final Writer writer, final FieldNode annotatedField) throws IOException {
        this.objectWriter.writerWithDefaultPrettyPrinter().writeValue(writer, annotatedField);
    }

    @Override
    public void writeSchemaToFile(final OutputStream os, final FieldNode annotatedField) throws IOException {
        this.objectWriter.writerWithDefaultPrettyPrinter().writeValue(os, annotatedField);
    }

    @Override
    protected LinkedHashMap<String, Object> convertArrayNode(FieldNode annotatedField) {
        // no need to parse anything
        return new LinkedHashMap<>();
    }

    @Override
    protected LinkedHashMap<String, Object> convertMapNode(FieldNode property) {
        // no need to parse anything
        return new LinkedHashMap<>();
    }

    @Override
    protected LinkedHashMap<String, Object> convertBooleanNode(FieldNode annotatedField) {
        // no need to parse anything
        return new LinkedHashMap<>();
    }

    @Override
    protected LinkedHashMap<String, Object> convertIntegerNode(FieldNode annotatedField) {
        // no need to parse anything
        return new LinkedHashMap<>();
    }

    @Override
    protected LinkedHashMap<String, Object> convertNumberNode(FieldNode annotatedField) {
        // no need to parse anything
        return new LinkedHashMap<>();
    }

    @Override
    protected LinkedHashMap<String, Object> convertObjectNode(FieldNode annotatedField) {
        // no need to parse anything
        return new LinkedHashMap<>();
    }

    @Override
    protected LinkedHashMap<String, Object> convertStringNode(FieldNode annotatedField) {
        // no need to parse anything
        return new LinkedHashMap<>();
    }

    @Override
    protected LinkedHashMap<String, Object> convertNullNode(FieldNode property) {
        // no need to parse anything
        return new LinkedHashMap<>();
    }

    @Override
    protected LinkedHashMap<String, Object> convertConfigRoot(FieldNode annotatedField) {
        return new LinkedHashMap<>();
    }
}
