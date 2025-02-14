package com.networknt.config.schema;

import com.networknt.config.schema.generator.Generator;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import java.lang.annotation.Annotation;
import java.util.Optional;

public class ReflectionUtils {

    /**
     * Safely gets an element from a canonical name.
     *
     * @param canonicalName The canonical name of the element.
     * @param processingEnvironment The processing environment to use.
     * @return The element if it exists, otherwise null.
     */
    public static Element safeGetElement(final String canonicalName, final ProcessingEnvironment processingEnvironment) {
        try {
            return processingEnvironment.getElementUtils().getTypeElement(canonicalName);
        } catch (final MirroredTypeException e) {
            return processingEnvironment.getTypeUtils().asElement(e.getTypeMirror());
        }
    }

    public static void printName(Generator generator) {
        System.out.println(generator.getClass().getName());
    }

    /**
     * Safely gets an annotation from an element.
     *
     * @param element The element to get the annotation from.
     * @param annotationClass The annotation class to get.
     * @param <A> The type of the annotation.
     * @return The annotation if it exists, otherwise an empty optional.
     */
    public static <A extends Annotation> Optional<A> safeGetAnnotation(final Element element, final Class<A> annotationClass) {
        return safeGetAnnotation(element, annotationClass, null);
    }

    /**
     * Safely gets an annotation from an element.
     *
     * @param element The element to get the annotation from.
     * @param annotationClass The annotation class to get.
     * @param processingEnvironment The processing environment to use.
     * @param <A> The type of the annotation.
     * @return The annotation if it exists, otherwise an empty optional.
     */
    public static <A extends Annotation> Optional<A> safeGetAnnotation(final Element element, final Class<A> annotationClass, final ProcessingEnvironment processingEnvironment) {

        if (element == null || annotationClass == null)
            return Optional.empty();

        try {

            final var annotation = element.getAnnotation(annotationClass);

            if (annotation == null)
                return Optional.empty();

            else return Optional.of(annotation);

        } catch (final MirroredTypeException e) {

            final var typeMirror = e.getTypeMirror();

            if (processingEnvironment == null)
                return Optional.empty();

            final var typeElement = (TypeElement) processingEnvironment.getTypeUtils().asElement(typeMirror);
            final var annotation = typeElement.getAnnotation(annotationClass);

            if (annotation == null)
                return Optional.empty();

            else return Optional.of(annotation);
        }
    }
}
