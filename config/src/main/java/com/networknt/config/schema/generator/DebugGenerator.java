package com.networknt.config.schema.generator;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
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
    public void writeSchemaToFile(String path, LinkedHashMap<String, Object> metadata) {
        try {
            final var file = new File(path + "/" + configKey + ".debug.json");
            this.objectWriter.writerWithDefaultPrettyPrinter().writeValue(file, metadata);
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
