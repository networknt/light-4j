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
     * The externalized key name of the field.
     * If set, the value of the field will be formatted in the Light4J configuration style.
     * i.e.
     * ${configKeyName.externalizedKeyName:defaultValue}
     */
    String externalizedKeyName() default ConfigSchema.DEFAULT_STRING;

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
    Class<?> ref();
}
