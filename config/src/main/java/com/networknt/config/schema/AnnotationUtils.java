package com.networknt.config.schema;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;

public class AnnotationUtils {

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

    public static boolean elementIsClass(
            final Element element,
            final Class<?> clazz,
            final ProcessingEnvironment processingEnvironment
    ) {
        final var elementClass = AnnotationUtils.getClassFromElement(element, processingEnvironment);
        return elementClass.map(aClass -> aClass.equals(clazz)).orElse(false);
    }

    public static boolean elementImplementsClass(final Element element, final Class<?> clazz, final ProcessingEnvironment processingEnv) {
        final var elementClass = AnnotationUtils.getClassFromElement(element, processingEnv);
        if (elementClass.isEmpty())
            return false;

        for (final var implementedInterface : elementClass.get().getInterfaces()) {
            if (implementedInterface.equals(clazz)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets the full canonical name from the element and gets the class from the string.
     *
     * @param element -
     * @param processingEnv
     * @return
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
     * Updates the property if the field is not the default value.
     *
     * @param field        The field to update.
     * @param property     The property to update.
     * @param key          The key to update.
     * @param defaultValue The default value.
     * @param type         The class type of the value.
     * @param <T>          The type of the value.
     */
    public static <T> void updateIfNotDefault(
            final LinkedHashMap<String, Object> field,
            final LinkedHashMap<String, Object> property,
            final String key,
            final Object defaultValue,
            final Class<T> type
    ) {
        final var value = getAsType(field.get(key), type);
        updateIfNotDefault(property, value, key, defaultValue);
    }

    /**
     * Updates the property if the field is not the default value.
     *
     * @param property     The property to update.
     * @param key          The key to update.
     * @param defaultValue The default value.
     * @param <T>          The type of the value.
     */
    public static <T> void updateIfNotDefault(
            final LinkedHashMap<String, Object> property,
            final T value,
            final String key,
            final Object defaultValue
    ) {
        if (value != null && !Objects.equals(value, defaultValue))
            property.put(key, value);
    }

    /**
     * Casts the provided value to a specific class.
     *
     * @param value The value to cast.
     * @param type  The class to cast to.
     * @param <T>   The type to cast to.
     * @return The cast value.
     */
    public static <T> T getAsType(final Object value, final Class<T> type) {
        if (value == null)
            return null;

        if (type.isInstance(value))
            return type.cast(value);

        else return null;
    }

    public static void logError(String message, Object... args) {
        final var errorPrefix = "[ERROR] ";
        final var formattedMessage = formatMessage(errorPrefix + message, args);
        System.out.println(formattedMessage);
    }

    public static void logWarning(String message, Object... args) {
        final var warningPrefix = "[WARNING] ";
        final var formattedMessage = formatMessage(warningPrefix + message, args);
        System.out.println(formattedMessage);
    }

    public static void logInfo(String message, Object... args) {
        final var warningPrefix = "[INFO] ";
        final var formattedMessage = formatMessage(warningPrefix + message, args);
        System.out.println(formattedMessage);
    }


    private static String formatMessage(String message, Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }

        return String.format(message, args);
    }
}
