package com.networknt.config.schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MapField {

    /**
     * The key name found in the configuration file.
     */
    String configFieldName();

    /**
     * The default json map value of the field.
     */
    String defaultValue() default "";

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
     * The additionalProperties flag of the field.
     * If set to true, the map can have additional properties.
     */
    boolean additionalProperties() default false;

    /**
     * The keyType of the map.
     */
    Class<?> keyType() default String.class;

    /**
     * The valueType of the map.
     */
    Class<?> valueType() default String.class;
}
