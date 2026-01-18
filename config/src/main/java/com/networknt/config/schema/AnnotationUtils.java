package com.networknt.config.schema;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

public class AnnotationUtils {

    private AnnotationUtils() {
        throw new IllegalStateException("AnnotationUtils is a utility class");
    }

    /**
     * Safely gets an element from a canonical name.
     *
     * @param name             - The canonical name of the element.
     * @param pe               - The processing environment to use.
     * @return                 - Returns the element if it exists, otherwise none.
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

    /**
     * Safely accesses Class[] type members of an annotation class.
     *
     * @param element               - The annotated element.
     * @param annotationClass       - The annotation class containing Class[]
     * @param memberName            - The method in from the annotation class to access the class array.
     * @param processingEnvironment - The current annotation processing environment
     * @return                      - Returns a list of type mirrors for. None if it was not found.
     */
    public static Optional<List<TypeMirror>> getClassArrayMirrors(
            final Element element,
            final Class<? extends Annotation> annotationClass,
            final String memberName,
            final ProcessingEnvironment processingEnvironment
    ) {
        final var elements = processingEnvironment.getElementUtils();
        final var annotationMirror = element.getAnnotationMirrors()
                .stream()
                .filter(mirror -> mirror.getAnnotationType().toString().equals(annotationClass.getCanonicalName()))
                .findFirst();

        if (annotationMirror.isEmpty()) {
            return Optional.empty();
        }

        final var elementValues = elements.getElementValuesWithDefaults(annotationMirror.get());

        return elementValues
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().getSimpleName().toString().equals(memberName))
                .findFirst()
                .map(entry -> {
                    AnnotationValue valueWrapper = entry.getValue();
                    @SuppressWarnings("unchecked")
                    List<? extends AnnotationValue> values = (List<? extends AnnotationValue>) valueWrapper.getValue();
                    return values.stream()
                            .map(v -> (TypeMirror) v.getValue())
                            .collect(Collectors.toList());
                });
    }

}
