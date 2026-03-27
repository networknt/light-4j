package com.networknt.config.schema;

import java.lang.annotation.*;

/**
 * Annotation for integer fields in configuration schemas.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IntegerField {

    /**
     * The key name found in the configuration file.
     *
     * @return String config field name
     */
    String configFieldName();

    /**
     * The default integer value of the field.
     *
     * @return String default value
     */
    String defaultValue() default ConfigSchema.DEFAULT_STRING;

    /**
     * The description of the field.
     *
     * @return String description
     */
    String description() default ConfigSchema.DEFAULT_STRING;

    /**
     * The externalized key name of the field.
     * If set, the value of the field will be formatted in the Light4J configuration style.
     * i.e.
     * ${configKeyName.externalizedKeyName:defaultValue}
     *
     * @return String externalized key name
     */
    String externalizedKeyName() default ConfigSchema.DEFAULT_STRING;

    /**
     * The minimum value of the field.
     *
     * @return int min value
     */
    int min() default ConfigSchema.DEFAULT_MIN_INT;

    /**
     * The maximum value of the field.
     *
     * @return int max value
     */
    int max() default ConfigSchema.DEFAULT_MAX_INT;

    /**
     * The exclusiveMin flag of the field.
     * If set to true, the value of the field must be greater than the minimum value.
     *
     * @return boolean exclusive min
     */
    boolean exclusiveMin() default ConfigSchema.DEFAULT_BOOLEAN;

    /**
     * The exclusiveMax flag of the field.
     * If set to true, the value of the field must be less than the maximum value.
     *
     * @return boolean exclusive max
     */
    boolean exclusiveMax() default ConfigSchema.DEFAULT_BOOLEAN;

    /**
     * The multipleOf value of the field.
     * If set, the value of the field must be a multiple of this value.
     *
     * @return int multiple of
     */
    int multipleOf() default ConfigSchema.DEFAULT_INT;

    /**
     * The format of the field.
     *
     * @return Format format
     */
    Format format() default Format.int32;
}
