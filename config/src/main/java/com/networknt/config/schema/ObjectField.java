package com.networknt.config.schema;

import java.lang.annotation.*;


@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ObjectField {

    /**
     * The key name found in the configuration file.
     */
    String configFieldName();

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
     * The default value of the field. For objects, this must be in JSON string format.
     * i.e.
     * {\"exampleField\": \"exampleValue\"}
     */
    String defaultValue() default ConfigSchema.DEFAULT_STRING;

    /**
     * The default object value of the field.
     */
    boolean useSubObjectDefault() default ConfigSchema.DEFAULT_BOOLEAN;

    /**
     * The reference class of the field.
     */
    Class<?> ref() default Object.class;

    /**
     * The required flag of the field.
     * If set, all classes must be present in the configuration file.
     */
    Class<?>[] allOf() default {};

    /**
     * The required flag of the field.
     * If set to true, any class must be present in the configuration file.
     */
    Class<?>[] anyOf() default {};

    /**
     * The required flag of the field.
     * If set, only one class must be present in the configuration file.
     */
    Class<?>[] oneOf() default {};
}
