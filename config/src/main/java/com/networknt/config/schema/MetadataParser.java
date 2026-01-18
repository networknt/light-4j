package com.networknt.config.schema;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import java.lang.annotation.Annotation;
import java.util.*;


/**
 * A parser that reads metadata from annotations and bundles it into a LinkedHashMap.
 * Bundled metadata is then used by generators to generate different schema types (yaml, json, etc.).
 *
 * @author Kalev Gonvick
 */
public class MetadataParser {

    public static final String INTEGER_TYPE = "integer";
    public static final String NUMBER_TYPE = "number";
    public static final String STRING_TYPE = "string";
    public static final String BOOLEAN_TYPE = "boolean";
    public static final String URL_TYPE = "url";
    public static final String URI_TYPE = "uri";

    private static final FieldNode DEFAULT_CONTAINER_PROPS = FieldNode.defaultNode();

    private MetadataParser() {
        throw new IllegalStateException("MetadataParser should not be instantiated.");
    }

    /**
     * Gathers schema data for an object element.
     *
     * @param currentRoot The current root element.
     * @param pe          The processing environment.
     */
    protected static FieldNode.Builder gatherObjectSchemaData(
            final Element currentRoot,
            final ProcessingEnvironment pe
    ) {

        final var name = currentRoot.getSimpleName().toString().toLowerCase();

        // For all the enclosed elements in the current root, check to see if there are any annotations we can parse.
        final var fields = currentRoot.getEnclosedElements();
        final var childNodes = new ArrayList<FieldNode>();
        fields.forEach(field -> resolveAnnotationData(field, pe).ifPresent(childNodes::add));

        final FieldNode.Builder builder;
        if (AnnotationUtils.isRelated(currentRoot, Collection.class, pe)) {
            builder = new FieldNode.Builder(FieldType.ARRAY, name);
            if (childNodes.isEmpty())
                builder.childNodes(List.of(DEFAULT_CONTAINER_PROPS));

        } else if (AnnotationUtils.isRelated(currentRoot, Map.class, pe)) {
            builder = new FieldNode.Builder(FieldType.MAP, name);
            if (childNodes.isEmpty())
                builder.childNodes(List.of(DEFAULT_CONTAINER_PROPS));

            /* Handle raw java types */
        } else {
            switch (name) {

                /* types with no formats */
                case BOOLEAN_TYPE: {
                    var type = FieldType.BOOLEAN;
                    builder = new FieldNode.Builder(type, name);
                    break;
                }
                case INTEGER_TYPE: {
                    builder = FieldType.INTEGER.newBuilder(name);
                    break;
                }
                case NUMBER_TYPE: {
                    builder = FieldType.NUMBER.newBuilder(name);
                    break;
                }
                case STRING_TYPE: {
                    builder = FieldType.STRING.newBuilder(name);
                    break;
                }

                /* types with formats */
                case URI_TYPE:
                case URL_TYPE: {
                    builder = FieldType.STRING.newBuilder(name).format(Format.uri);
                    break;
                }

                /* fallback */
                default: {
                    var type = FieldType.OBJECT;
                    builder = new FieldNode.Builder(type, name);
                    break;
                }
            }
        }

        if (!childNodes.isEmpty()) {
            builder.childNodes(childNodes);
        }

        if (currentRoot instanceof javax.lang.model.element.TypeElement) {
            final var typeElement = (TypeElement) currentRoot;
            builder.className(typeElement.getQualifiedName().toString());
        }

        return builder;
    }

    /**
     * Checks to see if any of the supported annotations are found on the current field.
     * If the element has no annotations matching the list, just return empty.
     *
     * @param annotatedClassField   - The current field being processed.
     * @param processingEnvironment - The current processing environment.
     * @return - Returns the field node containing the data found in the annotation, returns none if no annotation was found.
     */
    private static Optional<FieldNode> resolveAnnotationData(
            final Element annotatedClassField,
            final ProcessingEnvironment processingEnvironment
    ) {
        return AnnotationUtils.getAnnotation(annotatedClassField, BooleanField.class, processingEnvironment)
                .map(MetadataParser::parseBooleanMetadata)
                .or(() -> AnnotationUtils.getAnnotation(annotatedClassField, IntegerField.class, processingEnvironment)
                        .map(MetadataParser::parseIntegerMetadata))
                .or(() -> AnnotationUtils.getAnnotation(annotatedClassField, NullField.class, processingEnvironment)
                        .map(MetadataParser::parseNullMetadata))
                .or(() -> AnnotationUtils.getAnnotation(annotatedClassField, ObjectField.class, processingEnvironment)
                        .map(annotation -> parseObjectMetadata(annotatedClassField, annotation, processingEnvironment)))
                .or(() -> AnnotationUtils.getAnnotation(annotatedClassField, StringField.class, processingEnvironment)
                        .map(MetadataParser::parseStringMetadata))
                .or(() -> AnnotationUtils.getAnnotation(annotatedClassField, NumberField.class, processingEnvironment)
                        .map(MetadataParser::parseNumberMetadata))
                .or(() -> AnnotationUtils.getAnnotation(annotatedClassField, ArrayField.class, processingEnvironment)
                        .map(annotation -> parseArrayMetadata(annotatedClassField, annotation, processingEnvironment)))
                .or(() -> AnnotationUtils.getAnnotation(annotatedClassField, MapField.class, processingEnvironment)
                        .map(annotation -> parseMapMetadata(annotatedClassField, annotation, processingEnvironment)));
    }

    /**
     * Parses the 'ArrayField' annotation and returns a FieldNode containing the metadata.
     * Review the ArrayField interface to view all available fields.
     *
     * @param element               The field that is annotated with 'ArrayField'
     * @param field                 The field of the config to parse.
     * @param processingEnvironment The processing environment to resolve subtypes.
     * @return A FieldNode containing the metadata.
     */
    private static FieldNode parseArrayMetadata(
            final Element element,
            final ArrayField field,
            final ProcessingEnvironment processingEnvironment
    ) {
        final var builder = new FieldNode.Builder(FieldType.ARRAY, field.configFieldName());
        var parsed = handleReferenceClassArray(element, ArrayField.class, "itemsOneOf", processingEnvironment)
                .map(builder::oneOf)
                .or(() -> handleReferenceClassArray(element, ArrayField.class, "itemsAllOf", processingEnvironment)
                        .map(builder::allOf))
                .or(() -> handleReferenceClassArray(element, ArrayField.class, "itemsAnyOf", processingEnvironment)
                        .map(builder::anyOf));
        if (parsed.isEmpty()) {
            String canonicalName;
            try {
                canonicalName = field.items().getCanonicalName();
            } catch (MirroredTypeException e) {
                canonicalName = e.getTypeMirrors().get(0).toString();
            }
            AnnotationUtils.getElement(canonicalName, processingEnvironment).ifPresent(ref -> {
                var data = gatherObjectSchemaData(ref, processingEnvironment).build();
                builder.ref(data);
            });
        }
        return builder.externalizedKeyName(field.externalizedKeyName())
                .description(field.description())
                .minItems(field.minItems())
                .maxItems(field.maxItems())
                .uniqueItems(field.uniqueItems())
                .contains(field.contains())
                .subObjectDefault(field.useSubObjectDefault())
                .defaultValue(field.defaultValue())
                .build();
    }

    private static FieldNode parseMapMetadata(
            final Element element,
            final MapField field,
            final ProcessingEnvironment pe
    ) {
        final var builder = FieldType.MAP.newBuilder(field.configFieldName());
        var parsed = handleReferenceClassArray(element, MapField.class, "valueTypeOneOf", pe)
                .map(builder::oneOf)
                .or(() -> handleReferenceClassArray(element, MapField.class, "valueTypeAllOf", pe)
                        .map(builder::allOf))
                .or(() -> handleReferenceClassArray(element, MapField.class, "valueTypeAnyOf", pe)
                        .map(builder::anyOf));
        if (parsed.isEmpty()) {
            String canonicalName;
            try {
                canonicalName = field.valueType().getCanonicalName();
            } catch (MirroredTypeException e) {
                canonicalName = e.getTypeMirrors().get(0).toString();
            }
            AnnotationUtils.getElement(canonicalName, pe).ifPresent(ref -> {
                var data = gatherObjectSchemaData(ref, pe).build();
                builder.ref(data);
            });
        }
        return builder.externalizedKeyName(field.externalizedKeyName())
                .description(field.description())
                .defaultValue(field.defaultValue())
                .build();
    }

    /**
     * Parses the 'IntegerField' annotation and returns a FieldNode containing the metadata.
     * Review the IntegerField interface to view all available fields.
     *
     * @param field The field of the config to parse.
     * @return A FieldNode containing the metadata.
     */
    private static FieldNode parseIntegerMetadata(final IntegerField field) {
        return FieldType.INTEGER.newBuilder(field.configFieldName())
                .description(field.description())
                .externalizedKeyName(field.externalizedKeyName())
                .defaultValue(field.defaultValue())
                .min(field.min())
                .max(field.max())
                .exclusiveMax(field.exclusiveMax())
                .exclusiveMin(field.exclusiveMin())
                .multipleOf(field.multipleOf())
                .format(field.format())
                .build();
    }

    /**
     * Parses the 'NumberField' annotation and returns a LinkedHashMap containing the metadata.
     * Review the NumberField interface to view all available fields.
     *
     * @param field The field of the config to parse.
     * @return A LinkedHashMap containing the metadata.
     */
    private static FieldNode parseNumberMetadata(final NumberField field) {
        return FieldType.NUMBER.newBuilder(field.configFieldName())
                .externalizedKeyName(field.externalizedKeyName())
                .description(field.description())
                .defaultValue(field.defaultValue())
                .min(field.min())
                .max(field.max())
                .exclusiveMin(field.exclusiveMin())
                .exclusiveMax(field.exclusiveMax())
                .multipleOf(field.multipleOf())
                .format(field.format())
                .build();
    }

    /**
     * Parses the 'StringField' annotation and returns a FieldNode containing the metadata.
     * Review the StringField interface to view all available fields.
     *
     * @param field The field of the config to parse.
     * @return A FieldNode containing the metadata.
     */
    private static FieldNode parseStringMetadata(final StringField field) {
        return FieldType.STRING.newBuilder(field.configFieldName())
                .externalizedKeyName(field.externalizedKeyName())
                .description(field.description())
                .minLength(field.minLength())
                .maxLength(field.maxLength())
                .defaultValue(field.defaultValue())
                .pattern(field.pattern())
                .format(field.format())
                .build();
    }

    private static <A extends Annotation> Optional<List<FieldNode>> handleReferenceClassArray(
            final Element element,
            final Class<A> annotationClass,
            final String memberName,
            final ProcessingEnvironment pe
    ) {
        return AnnotationUtils.getClassArrayMirrors(element, annotationClass, memberName, pe)
                .filter(list -> !list.isEmpty())
                .map(list -> {
                    final var dataList = new ArrayList<FieldNode>();
                    list.forEach(mirror -> {
                        AnnotationUtils.getElement(mirror.toString(), pe).ifPresent(el -> {
                            var fieldNode = gatherObjectSchemaData(el, pe).build();
                            dataList.add(fieldNode);
                        });
                    });
                    return dataList;
                });
    }

    /**
     * Parses the 'ObjectField' annotation and returns a FieldNode containing the metadata.
     * Review the ObjectField interface to view all available fields.
     *
     * @param element
     * @param field
     * @param pe
     * @return
     */
    private static FieldNode parseObjectMetadata(
            final Element element,
            final ObjectField field,
            final ProcessingEnvironment pe
    ) {
        var builder = FieldType.OBJECT.newBuilder(field.configFieldName());
        var parsed = handleReferenceClassArray(element, ObjectField.class, "refOneOf", pe)
                .map(builder::oneOf)
                .or(() -> handleReferenceClassArray(element, ObjectField.class, "refAllOf", pe)
                        .map(builder::allOf))
                .or(() -> handleReferenceClassArray(element, ObjectField.class, "refAnyOf", pe)
                        .map(builder::anyOf));

        if (parsed.isEmpty()) {
            String canonicalName;
            try {
                canonicalName = field.ref().getCanonicalName();
            } catch (MirroredTypeException e) {
                canonicalName = e.getTypeMirrors().get(0).toString();
            }
            AnnotationUtils.getElement(canonicalName, pe).ifPresent(ref -> {
                var data = gatherObjectSchemaData(ref, pe).build();
                builder.ref(data);
            });
        }
        return builder.externalizedKeyName(field.externalizedKeyName())
                .description(field.description())
                .defaultValue(field.defaultValue())
                .subObjectDefault(field.useSubObjectDefault())
                .build();

    }

    /**
     * Parses the 'NullField' annotation and returns a FieldNode containing the metadata.
     * Review the NullField interface to view all available fields.
     *
     * @param field The field of the config to parse.
     * @return A LinkedHashMap containing the metadata.
     */
    private static FieldNode parseNullMetadata(final NullField field) {
        return FieldType.NULL.newBuilder(field.configFieldName())
                .externalizedKeyName(field.externalizedKeyName())
                .description(field.description())
                .defaultValue(field.defaultValue())
                .build();
    }

    /**
     * Parses the 'BooleanField' annotation and returns a FieldNode containing the metadata.
     * Review the BooleanField interface to view all available fields.
     *
     * @param field The field of the config to parse.
     * @return A FieldNode containing the metadata.
     */
    private static FieldNode parseBooleanMetadata(final BooleanField field) {
        return FieldType.BOOLEAN.newBuilder(field.configFieldName())
                .externalizedKeyName(field.externalizedKeyName())
                .description(field.description())
                .defaultValue(field.defaultValue())
                .build();
    }
}
