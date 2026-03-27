package com.networknt.config.schema;

import java.lang.annotation.*;

/**
 * Annotation for boolean fields in configuration schemas.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BooleanField {

    /**
     * The key name found in the configuration file.
     *
     * @return String config field name
     */
    String configFieldName();

    /**
     * The default boolean value of the field.
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
}
