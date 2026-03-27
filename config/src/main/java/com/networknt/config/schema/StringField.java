package com.networknt.config.schema;

import java.lang.annotation.*;

/**
 * Annotation for string fields in configuration schemas.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface StringField {

    /**
     * The key name found in the configuration file.
     *
     * @return String config field name
     */
    String configFieldName();

    /**
     * The default string value of the field.
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
     * The minimum length of the field.
     *
     * @return int min length
     */
    int minLength() default ConfigSchema.DEFAULT_INT;

    /**
     * The maximum length of the field.
     *
     * @return int max length
     */
    int maxLength() default ConfigSchema.DEFAULT_MAX_INT;

    /**
     * The pattern of the field.
     *
     * @return String pattern
     */
    String pattern() default ConfigSchema.DEFAULT_STRING;

    /**
     * The format of the field.
     *
     * @return Format format
     */
    Format format() default Format.none;

    /**
     * The injection indicator of the field.
     *
     * @return boolean injection
     */
    boolean injection() default ConfigSchema.DEFAULT_BOOLEAN_TRUE;
}
