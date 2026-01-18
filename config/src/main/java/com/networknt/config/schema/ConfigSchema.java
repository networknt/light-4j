package com.networknt.config.schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigSchema {

    /**
     * Default value for optional annotation strings.
     */
    String DEFAULT_STRING = "";

    /**
     * Default value for optional annotation integers that cannot be less than 0.
     */
    int DEFAULT_INT = 0;

    /**
     * Default value for annotation integers that involve ranges.
     * Represents the upper integer range.
     */
    int DEFAULT_MAX_INT = Integer.MAX_VALUE;

    /**
     * Default value for annotation integers that involve ranges.
     * Represents the lower integer range.
     */
    int DEFAULT_MIN_INT = Integer.MIN_VALUE;

    /**
     * Default value for optional annotation numbers that cannot be less than 0.0.
     */
    double DEFAULT_NUMBER = 0.0;

    /**
     * Default value for annotation numbers that involve ranges.
     * Represents the upper number range.
     */
    double DEFAULT_MAX_NUMBER = Double.MAX_VALUE;

    /**
     * Default value for annotation numbers that involve ranges.
     * Represents the lower number range.
     */
    double DEFAULT_MIN_NUMBER = Double.MIN_VALUE;

    /**
     * Default value for optional annotation booleans.
     */
    boolean DEFAULT_BOOLEAN = false;

    /**
     * The name of the parameter key of the configuration file.
     */
    String configKey();

    /**
     * The name of the configuration file.
     */
    String configName();

    /**
     * List of all the configuration file formats generated.
     * Leaving this empty means no configuration files get written.
     */
    OutputFormat[] outputFormats() default {};

    /**
     * The description of the config file itself.
     */
    String configDescription() default DEFAULT_STRING;
}
