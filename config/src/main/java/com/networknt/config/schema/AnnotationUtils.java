package com.networknt.config.schema;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import java.lang.annotation.Annotation;
import java.util.*;

/**
 * Utility class for annotation processing.
 */
public class AnnotationUtils {

    private AnnotationUtils() {
        throw new IllegalStateException("AnnotationUtils is a utility class");
    }

    /**
     * Safely gets an element from a canonical name.
     *
     * @param name The canonical name of the element.
     * @param pe   The processing environment to use.
     * @return     Returns the element if it exists, otherwise none.
     */
    public static Optional<Element> getElement(
            final String name,
            final ProcessingEnvironment pe
    ) {
        try {
            return Optional.ofNullable(pe.getElementUtils().getTypeElement(name));
        } catch (final MirroredTypeException e) {
            return Optional.ofNullable(pe.getTypeUtils().asElement(e.getTypeMirror()));
        }
    }

    /**
     * Checks to see if the provided element is a specific class, or if the element inherits from the specific class.
     *
     * @param element               - The element to check.
     * @param clazz                 - The class to compare to.
     * @param pe                    - The current processing environment.
     * @return                      - Returns true if element is the class or inherits from it.
     */
    public static boolean isRelated(
            final Element element,
            final Class<?> clazz,
            final ProcessingEnvironment pe
    ) {
        return AnnotationUtils.getClassFromElement(element, pe)
                .map(Class::getInterfaces).stream().flatMap(Arrays::stream).anyMatch(clazz::equals)
                || AnnotationUtils.getClassFromElement(element, pe)
                .map(aClass -> aClass.equals(clazz)).orElse(false);
    }

    /**
     * Gets the full canonical name from the element and gets the class from the string.
     *
     * @param element               - The element to get the class from.
     * @param pe                    - The current processing environment
     * @return                      - Optionally returns the class for a given element. None if the element does not contain the class.
     */
    public static Optional<Class<?>> getClassFromElement(
            final Element element,
            final ProcessingEnvironment pe
    ) {
        final var typeElement = pe.getElementUtils().getTypeElement(element.toString());
        try {
            return Optional.of(Class.forName(typeElement.toString()));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

    /**
     * Safely gets an annotation from an element.
     *
     * @param element               - The element to get the annotation from.
     * @param annotationClass       - The annotation class to get.
     * @param processingEnvironment - The processing environment to use.
     * @param <A>                   - The type of the annotation.
     * @return                      - The annotation if it exists, otherwise an empty optional.
     */
    public static <A extends Annotation> Optional<A> getAnnotation(
            final Element element,
            final Class<A> annotationClass,
            final ProcessingEnvironment processingEnvironment
    ) {
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
