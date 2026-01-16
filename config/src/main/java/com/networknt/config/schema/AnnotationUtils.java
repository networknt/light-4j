package com.networknt.config.schema;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.*;

import static java.util.stream.Collectors.toList;

public class AnnotationUtils {

    private AnnotationUtils() {
        throw new IllegalStateException("AnnotationUtils is a utility class");
    }

    /**
     * Safely gets an element from a canonical name.
     *
     * @param canonicalName         The canonical name of the element.
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

    /**
     * Checks to see if the provided element is a specific class, or if the element inherits from the specific class.
     *
     * @param element - The element to check.
     * @param clazz - The class to compare to.
     * @param pe - The current processing environment.
     * @return - Returns true if element is the class or inherits from it.
     */
    public static boolean isRelatedToClass(
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
     * @param element       The element to get the class from.
     * @param processingEnv The current processing environment
     * @return - Optionally returns the class for a given element. None if the element does not contain the class.
     */
    public static Optional<Class<?>> getClassFromElement(final Element element, final ProcessingEnvironment processingEnv) {
        final var typeElement = processingEnv.getElementUtils().getTypeElement(element.toString());
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
    public static <A extends Annotation> Optional<A> safeGetAnnotation(
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
    public static Optional<List<TypeMirror>> getClassArrayValue(
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
                .filter(entry -> entry.getValue() instanceof List<?> && entry.getKey().getSimpleName().toString().equals(memberName))
                .findFirst()
                .map(entry -> ((List<?>) entry.getValue())
                        .stream()
                        .filter(AnnotationValue.class::isInstance)
                        .map(v -> (TypeMirror) ((AnnotationValue) v).getValue()).collect(toList()));
    }


}
