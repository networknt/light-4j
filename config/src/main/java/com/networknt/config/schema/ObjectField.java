package com.networknt.config.schema;

import java.lang.annotation.*;

/**
 * Annotation for object fields in configuration schemas.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ObjectField {

    /**
     * The key name found in the configuration file.
     *
     * @return String config field name
     */
    String configFieldName();

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
     * The default value of the field. For objects, this must be in JSON string format.
     * i.e.
     * {\"exampleField\": \"exampleValue\"}
     *
     * @return String default value
     */
    String defaultValue() default ConfigSchema.DEFAULT_STRING;

    /**
     * The default object value of the field.
     *
     * @return boolean use sub object default
     */
    boolean useSubObjectDefault() default ConfigSchema.DEFAULT_BOOLEAN;

    /**
     * The reference class of the field.
     *
     * @return Class reference class
     */
    Class<?> ref() default Object.class;

    /**
     * Array containing classes bound with an 'allOf' constraint.
     *
     * @return Class array ref all of
     */
    Class<?>[] refAllOf() default {};

    /**
     * Array containing classes bound with an 'onOf' constraint.
     *
     * @return Class array ref one of
     */
    Class<?>[] refOneOf() default {};

    /**
     * Array containing classes bound with an 'anyOf' constraint.
     *
     * @return Class array ref any of
     */
    Class<?>[] refAnyOf() default {};
}
