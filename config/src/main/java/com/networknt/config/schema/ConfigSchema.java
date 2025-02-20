package com.networknt.config.schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigSchema {

    String DEFAULT_STRING = "";
    int DEFAULT_INT = 0;
    int DEFAULT_MAX_INT = Integer.MAX_VALUE;
    int DEFAULT_MIN_INT = Integer.MIN_VALUE;
    double DEFAULT_NUMBER = 0.0;
    double DEFAULT_MAX_NUMBER = Double.MAX_VALUE;
    double DEFAULT_MIN_NUMBER = Double.MIN_VALUE;
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
}
