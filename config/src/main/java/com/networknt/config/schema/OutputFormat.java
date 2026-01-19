package com.networknt.config.schema;

import com.networknt.config.schema.generator.*;

/**
 * The supported output formats for configuration generation.
 */
public enum OutputFormat {

    /**
     * Draft 07 JSON schema output.
     */
    JSON_SCHEMA("-schema.json"),

    /**
     * Light4J YAML configuration output.
     */
    YAML(".yaml"),

    /**
     * Cloud Event stub output.
     */
    CLOUD(".cloud.json"),

    /**
     * Config generator debug output (just dumps the annotation processing results)
     */
    DEBUG(".debug.json");

    final String extension;

    OutputFormat(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }

    /**
     * Provides a generator depending on the output type.
     * This provides the mapping between the chosen generator and the implementation.
     *
     * @param configKey - The string key used when externalizing properties in.
     * @param configName - The name of the configuration file. The extension of the file is determined by the chosen output format.
     * @return - Returns a new generator.
     */
    public Generator getGenerator(final String configKey, final String configName) {
        switch (this) {
            case JSON_SCHEMA:
                return new JsonSchemaGenerator(configKey, configName);
            case YAML:
                return new YamlGenerator(configKey, configName);
            case CLOUD:
                return new CloudEventGenerator(configKey, configName);
            case DEBUG:
                return new DebugGenerator(configKey, configName);
            default:
                throw new IllegalArgumentException("Unsupported output format: " + this);
        }
    }
}
