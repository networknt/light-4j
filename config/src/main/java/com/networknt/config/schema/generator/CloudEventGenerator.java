package com.networknt.config.schema.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.networknt.config.schema.FieldNode;

import javax.tools.FileObject;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.LinkedHashMap;

/**
 * A generator that produces cloud event stubs for configuration classes.
 * <a href="https://github.com/cloudevents/spec">Cloud Event Spec</a>
 *
 * Most of this generator focuses on the 'metadata' of the config file
 * and less about 'metadata' of individual fields.
 *
 * @author Kalev Gonvick
 */
public class CloudEventGenerator extends Generator {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    static {
        OBJECT_MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
    }

    private static final String FAILED_PARSE_VALUE = "!FailedToParse";
    private static final String DEFAULT_SOURCE = "https://github.com/networknt/light4j";
    private static final String DEFAULT_CONFIG_TYPE = "Handler";
    private static final String DEFAULT_TYPE = "ConfigCreatedEvent";
    private static final String DEFAULT_DATA_TYPE = "application/json";
    private static final String DEFAULT_AGGREGATE_TYPE = "Config";

    public CloudEventGenerator(String configKey, String configName) {
        super(configKey, configName);
    }

    @Override
    public void writeSchemaToFile(FileObject object, FieldNode annotatedField) throws IOException {
        writeSchemaToFile(object.openOutputStream(), annotatedField);
    }

    @Override
    public void writeSchemaToFile(Writer writer, FieldNode annotatedField) throws IOException {
        final var cloudEvent = convertConfigRoot(annotatedField);
        OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(writer, cloudEvent);
    }

    @Override
    public void writeSchemaToFile(OutputStream os, FieldNode annotatedField) throws IOException {
        final var cloudEvent = convertConfigRoot(annotatedField);
        OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(os, cloudEvent);
    }


    @Override
    protected LinkedHashMap<String, Object> convertArrayNode(FieldNode annotatedField) {
        // Not needed for CloudEvent generation
        return new LinkedHashMap<>();
    }

    @Override
    protected LinkedHashMap<String, Object> convertBooleanNode(FieldNode annotatedField) {
        // Not needed for CloudEvent generation
        return new LinkedHashMap<>();
    }

    @Override
    protected LinkedHashMap<String, Object> convertIntegerNode(FieldNode annotatedField) {
        // Not needed for CloudEvent generation
        return new LinkedHashMap<>();
    }

    @Override
    protected LinkedHashMap<String, Object> convertNumberNode(FieldNode annotatedField) {
        // Not needed for CloudEvent generation
        return new LinkedHashMap<>();
    }

    @Override
    protected LinkedHashMap<String, Object> convertObjectNode(FieldNode annotatedField) {
        // Not needed for CloudEvent generation
        return new LinkedHashMap<>();
    }

    @Override
    protected LinkedHashMap<String, Object> convertStringNode(FieldNode annotatedField) {
        // Not needed for CloudEvent generation
        return new LinkedHashMap<>();
    }

    @Override
    protected LinkedHashMap<String, Object> convertNullNode(FieldNode property) {
        // Not needed for CloudEvent generation
        return new LinkedHashMap<>();
    }

    @Override
    protected LinkedHashMap<String, Object> convertMapNode(FieldNode property) {
        // Not needed for CloudEvent generation
        return new LinkedHashMap<>();
    }

    /**
     * Builds a CloudEvent structure based on the metadata.
     * @param annotatedField The metadata from the config annotations.
     * @return A LinkedHashMap representing the CloudEvent structure.
     */
    @Override
    protected LinkedHashMap<String, Object> convertConfigRoot(FieldNode annotatedField) {
        final var cloudEvent = new LinkedHashMap<String, Object>();

        // Create inner data object.
        final var data = getStringObjectLinkedHashMap(annotatedField);

        // Create outer cloud event structure including data.
        cloudEvent.put("id", "");
        cloudEvent.put("data", data);
        cloudEvent.put("host", "");
        cloudEvent.put("time", "");
        cloudEvent.put("type", DEFAULT_TYPE);
        cloudEvent.put("user", "");
        cloudEvent.put("nonce", "");
        cloudEvent.put("source", DEFAULT_SOURCE);
        cloudEvent.put("subject", "");
        cloudEvent.put("specversion", "");
        cloudEvent.put("aggregatetype", DEFAULT_AGGREGATE_TYPE);
        cloudEvent.put("datacontenttype", DEFAULT_DATA_TYPE);
        cloudEvent.put("aggregateversion", "");
        return cloudEvent;
    }

    private LinkedHashMap<String, Object> getStringObjectLinkedHashMap(FieldNode annotatedField) {
        final var data = new LinkedHashMap<String, Object>();
        data.put("configId", "");
        data.put("updateTs", "");
        var className = annotatedField.getOptionalSimpleClassName().orElse(FAILED_PARSE_VALUE);
        data.put("classPath", className);

        final var rootDescription = annotatedField.getOptionalDescription().orElse("");
        data.put("configDesc", rootDescription);

        data.put("configName", this.configName);
        data.put("configType", DEFAULT_CONFIG_TYPE);
        data.put("updateUser", "");
        data.put("configPhase", "");
        return data;
    }
}
