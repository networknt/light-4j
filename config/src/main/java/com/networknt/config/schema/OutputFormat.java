package com.networknt.config.schema;

/**
 * The supported output formats for configuration generation.
 */
public enum OutputFormat {

    /**
     * Draft 07 JSON schema output.
     */
    JSON_SCHEMA,

    /**
     * Light4J YAML configuration output.
     */
    YAML,

    /**
     * Config generator debug output (just dumps the annotation processing results)
     */
    DEBUG
}
