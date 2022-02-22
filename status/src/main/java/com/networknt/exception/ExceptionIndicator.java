package com.networknt.exception;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for handling exceptions in specific processor classes and/or
 * handler methods.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExceptionIndicator {

    /**
     * Exceptions handled by the annotated method.
     */
    Class<? extends Throwable>[] value() default {};
}
