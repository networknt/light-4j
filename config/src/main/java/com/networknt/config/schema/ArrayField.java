package com.networknt.config.schema;

import java.lang.annotation.*;

/**
 * Annotation for array fields in configuration schemas.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ArrayField {

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
     * The default value of the field. For arrays, this must be in JSON string format.
     * i.e.
     * [\"example\"]
     *
     * @return String default value
     */
    String defaultValue() default ConfigSchema.DEFAULT_STRING;

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
     * The useSubObjectDefault flag of the field.
     * If set to true, the default value of the subObject will be used instead of the current field.
     *
     * @return boolean use sub object default
     */
    boolean useSubObjectDefault() default ConfigSchema.DEFAULT_BOOLEAN;

    /**
     * The minimum number of items in this array.
     *
     * @return int min items
     */
    int minItems() default ConfigSchema.DEFAULT_INT;

    /**
     * The maximum number of items in this array.
     *
     * @return int max items
     */
    int maxItems() default ConfigSchema.DEFAULT_MAX_INT;

    /**
     * The uniqueItems flag of the field.
     * If set to true, all items in the array must be unique.
     *
     * @return boolean unique items
     */
    boolean uniqueItems() default ConfigSchema.DEFAULT_BOOLEAN;

    /**
     * The contains flag of the field.
     * If set to true, the array must contain at least one item that matches the schema.
     *
     * @return boolean contains
     */
    boolean contains() default ConfigSchema.DEFAULT_BOOLEAN;

    /**
     * The reference class of the field.
     *
     * @return Class items class
     */
    Class<?> items() default Object.class;

    /**
     * The items flag of the field.
     * If defined, the items in the array must match the schema.
     *
     * @return Class array items all of
     */
    Class<?>[] itemsAllOf() default {};

    /**
     * The items flag of the field.
     * If defined, the items in the array must match at least one of the schemas.
     *
     * @return Class array items any of
     */
    Class<?>[] itemsAnyOf() default {};

    /**
     * The items flag of the field.
     * If defined, the items in the array must match exactly one of the schemas.
     *
     * @return Class array items one of
     */
    Class<?>[] itemsOneOf() default {};
}
