package com.networknt.config.schema;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface StringField {

    /**
     * The key name found in the configuration file.
     */
    String configFieldName();

    /**
     * The default string value of the field.
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

    /**
     * The minimum length of the field.
     */
    int minLength() default ConfigSchema.DEFAULT_INT;

    /**
     * The maximum length of the field.
     */
    int maxLength() default ConfigSchema.DEFAULT_MAX_INT;

    /**
     * The pattern of the field.
     */
    String pattern() default ConfigSchema.DEFAULT_STRING;

    /**
     * The format of the field.
     */
    Format format() default Format.none;
}
