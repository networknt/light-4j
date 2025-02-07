package com.networknt.config.schema;

import java.lang.annotation.*;

@Documented
@Inherited
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
    boolean defaultValue() default ConfigSchema.DEFAULT_BOOLEAN;

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
}



