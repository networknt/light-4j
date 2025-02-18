package com.networknt.config.schema.generator;

import com.fasterxml.jackson.databind.ObjectMapper;

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

    public DebugGenerator(final String configKey) {
        super(configKey);
    }

    @Override
    public void writeSchemaToFile(final FileObject path, LinkedHashMap<String, Object> metadata) throws IOException {
        writeSchemaToFile(path.openOutputStream(), metadata);
    }

    @Override
    public void writeSchemaToFile(final Writer writer, final LinkedHashMap<String, Object> metadata) throws IOException {
        this.objectWriter.writerWithDefaultPrettyPrinter().writeValue(writer, metadata);
    }

    @Override
    public void writeSchemaToFile(final OutputStream os, final LinkedHashMap<String, Object> metadata) {
        try {
            this.objectWriter.writerWithDefaultPrettyPrinter().writeValue(os, metadata);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void parseArray(LinkedHashMap<String, Object> field, LinkedHashMap<String, Object> property) {
        // no need to parse anything
    }

    @Override
    protected void parseBoolean(LinkedHashMap<String, Object> field, LinkedHashMap<String, Object> property) {
        // no need to parse anything
    }

    @Override
    protected void parseInteger(LinkedHashMap<String, Object> field, LinkedHashMap<String, Object> property) {
        // no need to parse anything
    }

    @Override
    protected void parseNumber(LinkedHashMap<String, Object> field, LinkedHashMap<String, Object> property) {
        // no need to parse anything
    }

    @Override
    protected void parseObject(LinkedHashMap<String, Object> field, LinkedHashMap<String, Object> property) {
        // no need to parse anything
    }

    @Override
    protected void parseString(LinkedHashMap<String, Object> field, LinkedHashMap<String, Object> property) {
        // no need to parse anything
    }

    @Override
    protected void parseNullField(LinkedHashMap<String, Object> field, LinkedHashMap<String, Object> property) {
        // no need to parse anything
    }

    @Override
    protected LinkedHashMap<String, Object> getRootSchemaProperties(LinkedHashMap<String, Object> metadata) {
        return null;
    }
}
