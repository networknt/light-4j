package com.networknt.config.schema;

import java.lang.annotation.*;


@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IntegerField {

    /**
     * The key name found in the configuration file.
     */
    String configFieldName();

    /**
     * The default integer value of the field.
     */
    int defaultValue() default ConfigSchema.DEFAULT_INT;

    /**
     * The description of the field.
     */
    String description() default ConfigSchema.DEFAULT_STRING;

    /**
     * The externalized flag of the field.
     * If set to true, the value of the field will be formatted in the Light4J configuration style.
     * i.e.
     * ${configFileName.configFieldName:defaultValue}
     */
    boolean externalized() default ConfigSchema.DEFAULT_BOOLEAN;

    /**
     * The externalized key name of the field.
     * If set, the value of the field will be formatted in the Light4J configuration style.
     * i.e.
     * ${configKeyName.externalizedKeyName:defaultValue}
     */
    String externalizedKeyName() default ConfigSchema.DEFAULT_STRING;

    /**
     * The minimum value of the field.
     */
    int min() default ConfigSchema.DEFAULT_MIN_INT;

    /**
     * The maximum value of the field.
     */
    int max() default ConfigSchema.DEFAULT_MAX_INT;

    /**
     * The exclusiveMin flag of the field.
     * If set to true, the value of the field must be greater than the minimum value.
     */
    boolean exclusiveMin() default ConfigSchema.DEFAULT_BOOLEAN;

    /**
     * The exclusiveMax flag of the field.
     * If set to true, the value of the field must be less than the maximum value.
     */
    boolean exclusiveMax() default ConfigSchema.DEFAULT_BOOLEAN;

    /**
     * The multipleOf value of the field.
     * If set, the value of the field must be a multiple of this value.
     */
    int multipleOf() default ConfigSchema.DEFAULT_INT;

    /**
     * The format of the field.
     */
    Format format() default Format.int32;
}
