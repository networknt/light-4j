package com.networknt.config.schema.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.networknt.config.schema.AnnotationUtils;
import com.networknt.config.schema.MetadataParser;

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
    public void writeSchemaToFile(FileObject object, LinkedHashMap<String, Object> metadata) throws IOException {
        writeSchemaToFile(object.openOutputStream(), metadata);
    }

    @Override
    public void writeSchemaToFile(Writer writer, LinkedHashMap<String, Object> metadata) throws IOException {
        final var cloudEvent = getRootSchemaProperties(metadata);
        OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(writer, cloudEvent);
    }

    @Override
    public void writeSchemaToFile(OutputStream os, LinkedHashMap<String, Object> metadata) throws IOException {
        final var cloudEvent = getRootSchemaProperties(metadata);
        OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(os, cloudEvent);
    }


    @Override
    protected void parseArray(LinkedHashMap<String, Object> field, LinkedHashMap<String, Object> property) {
        // Not needed for CloudEvent generation
    }

    @Override
    protected void parseBoolean(LinkedHashMap<String, Object> field, LinkedHashMap<String, Object> property) {
        // Not needed for CloudEvent generation
    }

    @Override
    protected void parseInteger(LinkedHashMap<String, Object> field, LinkedHashMap<String, Object> property) {
        // Not needed for CloudEvent generation
    }

    @Override
    protected void parseNumber(LinkedHashMap<String, Object> field, LinkedHashMap<String, Object> property) {
        // Not needed for CloudEvent generation
    }

    @Override
    protected void parseObject(LinkedHashMap<String, Object> field, LinkedHashMap<String, Object> property) {
        // Not needed for CloudEvent generation
    }

    @Override
    protected void parseString(LinkedHashMap<String, Object> field, LinkedHashMap<String, Object> property) {
        // Not needed for CloudEvent generation
    }

    @Override
    protected void parseNullField(LinkedHashMap<String, Object> field, LinkedHashMap<String, Object> property) {
        // Not needed for CloudEvent generation
    }

    @Override
    protected void parseMapField(LinkedHashMap<String, Object> field, LinkedHashMap<String, Object> property) {
        // Not needed for CloudEvent generation
    }

    /**
     * Builds a CloudEvent structure based on the metadata.
     * @param metadata The metadata from the config annotations.
     * @return A LinkedHashMap representing the CloudEvent structure.
     */
    @Override
    protected LinkedHashMap<String, Object> getRootSchemaProperties(LinkedHashMap<String, Object> metadata) {
        final var cloudEvent = new LinkedHashMap<String, Object>();

        // Create inner data object.
        final var data = new LinkedHashMap<String, Object>();
        data.put("configId", "");
        data.put("updateTs", "");
        if (metadata.get(MetadataParser.CLASS_NAME_KEY) instanceof String) {
            data.put("classPath", metadata.get(MetadataParser.CLASS_NAME_KEY));
        } else {
            data.put("classPath", FAILED_PARSE_VALUE);
        }

        final var rootDescription = AnnotationUtils.getAsType(metadata.get(MetadataParser.DESCRIPTION_KEY), String.class);
        data.put("configDesc", rootDescription);

        data.put("configName", this.configName);
        data.put("configType", DEFAULT_CONFIG_TYPE);
        data.put("updateUser", "");
        data.put("configPhase", "");

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
}
