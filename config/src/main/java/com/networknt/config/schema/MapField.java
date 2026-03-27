package com.networknt.config.schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for map fields in configuration schemas.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MapField {

    /**
     * The key name found in the configuration file.
     *
     * @return String config field name
     */
    String configFieldName();

    /**
     * The default json map value of the field.
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
     * The additionalProperties flag of the field.
     * If set to true, the map can have additional properties.
     *
     * @return boolean additional properties
     */
    boolean additionalProperties() default false;

    /**
     * The value type of the map.
     *
     * @return Class value type
     */
    Class<?> valueType() default Object.class;

    /**
     * Represents a list of classes the value of the map contains.
     * Constrains the field to be 'anyOf' the classes in the list.
     *
     * @return Class array value type any of
     */
    Class<?>[] valueTypeAnyOf() default {};

    /**
     * Represents a list of classes the value of the map contains.
     * Constrains the field to be 'oneOf' the classes in the list.
     *
     * @return Class array value type one of
     */
    Class<?>[] valueTypeOneOf() default {};

    /**
     * Represents a list of classes the value of the map contains.
     * Constrains the field to be 'allOf' the classes in the list.
     *
     * @return Class array value type all of
     */
    Class<?>[] valueTypeAllOf() default {};

}
