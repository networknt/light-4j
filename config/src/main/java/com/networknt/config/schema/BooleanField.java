package com.networknt.config.schema;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BooleanField {

    /**
     * The key name found in the configuration file.
     */
    String configFieldName();

    /**
     * The default boolean value of the field.
     */
    String defaultValue() default ConfigSchema.DEFAULT_STRING;

    /**
     * The description of the field.
     */
    String description() default ConfigSchema.DEFAULT_STRING;

    /**
     * The externalized key name of the field.
     * If set, the value of the field will be formatted in the Light4J configuration style.
     * i.e.
     * ${configKeyName.externalizedKeyName:defaultValue}
     */
    String externalizedKeyName() default ConfigSchema.DEFAULT_STRING;
}
