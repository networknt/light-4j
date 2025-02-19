package com.networknt.config.schema;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NumberField {

    /**
     * The key name found in the configuration file.
     */
    String configFieldName();

    /**
     * The default number value of the field.
     */
    double defaultValue() default ConfigSchema.DEFAULT_NUMBER;

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
     * The minimum value of the field.
     */
    double min() default ConfigSchema.DEFAULT_MIN_NUMBER;

    /**
     * The maximum value of the field.
     */
    double max() default ConfigSchema.DEFAULT_MAX_NUMBER;

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
    double multipleOf() default ConfigSchema.DEFAULT_INT;

    /**
     * The format of the field.
     */
    Format format() default Format.float32;
}
