package com.networknt.config.schema;

/**
 * The supported output formats for configuration generation.
 */
public enum OutputFormat {

    /**
     * Draft 07 JSON schema output.
     */
    JSON_SCHEMA(".json"),

    /**
     * Light4J YAML configuration output.
     */
    YAML(".yaml"),

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
}
