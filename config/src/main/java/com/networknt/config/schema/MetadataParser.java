package com.networknt.config.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.util.Tuple;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.MirroredTypeException;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Function;


/**
 * A parser that reads metadata from annotations and bundles it into a LinkedHashMap.
 * Bundled metadata is then used by generators to generate different schema types (yaml, json, etc.).
 *
 * @author Kalev Gonvick
 */
public class MetadataParser {

    private static final Logger LOG = LoggerFactory.getLogger(MetadataParser.class);

    /* types */
    public static final String INTEGER_TYPE = "integer";
    public static final String NUMBER_TYPE = "number";
    public static final String STRING_TYPE = "string";
    public static final String OBJECT_TYPE = "object";
    public static final String ARRAY_TYPE = "array";
    public static final String NULL_TYPE = "null";
    public static final String BOOLEAN_TYPE = "boolean";
    public static final String MAP_TYPE = "map";

    /* formatted types */
    public static final String URL_TYPE = "url";
    public static final String URI_TYPE = "uri";

    /* keys */
    public static final String TYPE_KEY = "type";
    public static final String ID_KEY = "$id";
    public static final String DESCRIPTION_KEY = "description";
    public static final String EXTERNALIZED_KEY = "externalized";
    public static final String ADDITIONAL_PROPERTIES_KEY = "additionalProperties";
    public static final String DEFAULT_VALUE_KEY = "defaultValue";
    public static final String MINIMUM_KEY = "minimum";
    public static final String MAXIMUM_KEY = "maximum";
    public static final String REF_KEY = "ref";
    public static final String MIN_LENGTH_KEY = "minLength";
    public static final String MAX_LENGTH_KEY = "maxLength";
    public static final String PATTERN_KEY = "pattern";
    public static final String FORMAT_KEY = "format";
    public static final String ITEMS_KEY = "items";
    public static final String EXTERNALIZED_KEY_NAME = "externalizedKeyName";
    public static final String VALUE_TYPE_KEY = "valueType";
    public static final String CONFIG_FIELD_NAME_KEY = "configFieldName";
    public static final String USE_SUB_OBJECT_DEFAULT_KEY = "useSubObjectDefault";
    public static final String MIN_ITEMS_KEY = "minItems";
    public static final String MAX_ITEMS_KEY = "maxItems";
    public static final String UNIQUE_ITEMS_KEY = "uniqueItems";
    public static final String CONTAINS_KEY = "contains";
    public static final String EXCLUSIVE_MIN_KEY = "exclusiveMin";
    public static final String EXCLUSIVE_MAX_KEY = "exclusiveMax";
    public static final String MULTIPLE_OF_KEY = "multipleOf";
    public static final String PROPERTIES_KEY = "properties";

    private static final LinkedHashMap<String, String> DEFAULT_CONTAINER_PROPS = new LinkedHashMap<>();
    static {
        DEFAULT_CONTAINER_PROPS.put(TYPE_KEY, STRING_TYPE);
    }

    private final static LinkedHashMap<Class<? extends Annotation>, Function<Tuple<Annotation, ProcessingEnvironment>, LinkedHashMap<String, Object>>> FIELD_PARSE_FUNCTIONS = new LinkedHashMap<>();
    static {
        FIELD_PARSE_FUNCTIONS.put(BooleanField.class, (tuple) -> parseBooleanMetadata((BooleanField) tuple._1()));
        FIELD_PARSE_FUNCTIONS.put(IntegerField.class, (tuple) -> parseIntegerMetadata((IntegerField) tuple._1()));
        FIELD_PARSE_FUNCTIONS.put(NullField.class, (tuple) -> parseNullMetadata((NullField) tuple._1()));
        FIELD_PARSE_FUNCTIONS.put(ObjectField.class, (tuple) -> parseObjectMetadata((ObjectField) tuple._1(), tuple._2()));
        FIELD_PARSE_FUNCTIONS.put(StringField.class, (tuple) -> parseStringMetadata((StringField) tuple._1()));
        FIELD_PARSE_FUNCTIONS.put(NumberField.class, (tuple) -> parseNumberMetadata((NumberField) tuple._1()));
        FIELD_PARSE_FUNCTIONS.put(ArrayField.class, (tuple) -> parseArrayMetadata((ArrayField) tuple._1(), tuple._2()));
        FIELD_PARSE_FUNCTIONS.put(MapField.class, (tuple) -> parseMapMetadata((MapField) tuple._1(), tuple._2()));
    }

    /**
     * Parses the element for all config schema metadata.
     * @param element - the root element to gather metadata on.
     * @param processingEnvironment - the processing env.
     * @return - hashmap containing all data from annotations.
     */
    public LinkedHashMap<String, Object> parseMetadata(final Element element, final ProcessingEnvironment processingEnvironment) {
        final var rootMetadata = new LinkedHashMap<String, Object>();
        gatherObjectSchemaData(element, rootMetadata, processingEnvironment);
        return rootMetadata;
    }


    /**
     * Gathers schema data for an object element.
     * @param currentRoot The current root element.
     * @param rootMetadata The metadata to be populated.
     * @param processingEnvironment The processing environment.
     */
    private static void gatherObjectSchemaData(final Element currentRoot, final LinkedHashMap<String, Object> rootMetadata, final ProcessingEnvironment processingEnvironment) {

        LOG.trace("Gathering schema data for element: {}", currentRoot.getSimpleName());
        final var fields = currentRoot.getEnclosedElements();
        final var properties = new LinkedHashMap<String, Object>();
        final var name = currentRoot.getSimpleName().toString().toLowerCase();

        for (final var field : fields) {
            final var fieldMetadata = getObjectPropertyMetadata(field, processingEnvironment);

            if (fieldMetadata.isEmpty())
                continue;

            final var fieldName = fieldMetadata.get().get(CONFIG_FIELD_NAME_KEY).toString();
            properties.put(fieldName, fieldMetadata.get());
        }

        if (!properties.isEmpty())
            rootMetadata.put(PROPERTIES_KEY, properties);

        if (AnnotationUtils.elementImplementsClass(currentRoot, Collection.class, processingEnvironment)
                || AnnotationUtils.elementIsClass(currentRoot, Collection.class, processingEnvironment)) {

            rootMetadata.put(TYPE_KEY, ARRAY_TYPE);
            if (properties.isEmpty())
                rootMetadata.put(ITEMS_KEY, DEFAULT_CONTAINER_PROPS);

        } else if (AnnotationUtils.elementImplementsClass(currentRoot, Map.class, processingEnvironment)
                || AnnotationUtils.elementIsClass(currentRoot, Map.class, processingEnvironment)) {


            rootMetadata.put(TYPE_KEY, MAP_TYPE);
            if (properties.isEmpty())
                rootMetadata.put(ADDITIONAL_PROPERTIES_KEY, DEFAULT_CONTAINER_PROPS);

        } else {
            switch (name) {

                /* types with no formats */
                case BOOLEAN_TYPE:
                case INTEGER_TYPE:
                case NUMBER_TYPE:
                case STRING_TYPE: {
                    rootMetadata.put(TYPE_KEY, name);
                    break;
                }

                /* types with formats */
                case URI_TYPE:
                case URL_TYPE: {
                    rootMetadata.put(TYPE_KEY, STRING_TYPE);
                    rootMetadata.put(FORMAT_KEY, Format.uri.name());
                    break;
                }

                /* fallback */
                default: {
                    rootMetadata.put(TYPE_KEY, OBJECT_TYPE);
                    break;
                }
            }
        }
    }


    /**
     * Gets the metadata for a field element.
     * Depending on the element type, a different function is invoked to parse the info from the annotation.
     * @param element The element to get the metadata from.
     * @param processingEnvironment The processing environment to resolve MirrorTypes.
     * @return A LinkedHashMap containing the metadata.
     */
    private static Optional<LinkedHashMap<String, Object>> getObjectPropertyMetadata(final Element element, final ProcessingEnvironment processingEnvironment) {

        LOG.trace("Gathering object schema data for property: {}", element.getSimpleName());

        if (element.getKind() != ElementKind.FIELD)
            return Optional.empty();


        for (final var entry : FIELD_PARSE_FUNCTIONS.entrySet()) {
            final var annotationClass = entry.getKey();
            final var parseFunction = entry.getValue();

            if (AnnotationUtils.safeGetAnnotation(element, annotationClass, processingEnvironment).isPresent()) {
                final var annotation = AnnotationUtils.safeGetAnnotation(element, annotationClass, processingEnvironment).get();
                return Optional.of(parseFunction.apply(new Tuple<>(annotation, processingEnvironment)));
            }
        }

        return Optional.empty();
    }

    /**
     * Parses the 'ArrayField' annotation and returns a LinkedHashMap containing the metadata.
     * Review the ArrayField interface to view all available fields.
     *
     * @param field The field of the config to parse.
     * @param processingEnvironment The processing environment to resolve subtypes.
     * @return A LinkedHashMap containing the metadata.
     */
    private static LinkedHashMap<String, Object> parseArrayMetadata(final ArrayField field, final ProcessingEnvironment processingEnvironment) {
        String canonicalName;
        try {
            canonicalName = field.items().getCanonicalName();
        } catch (MirroredTypeException e) {
            canonicalName = e.getTypeMirrors().get(0).toString();
        }

        final var itemElement = AnnotationUtils.safeGetElement(canonicalName, processingEnvironment);
        final var itemMetadata = new LinkedHashMap<String, Object>();
        gatherObjectSchemaData(itemElement, itemMetadata, processingEnvironment);
        final var metadata = new LinkedHashMap<String, Object>();
        metadata.put(TYPE_KEY, ARRAY_TYPE);
        metadata.put(ID_KEY, getUUID());
        metadata.put(EXTERNALIZED_KEY_NAME, field.externalizedKeyName());
        metadata.put(CONFIG_FIELD_NAME_KEY, field.configFieldName());
        metadata.put(DESCRIPTION_KEY, field.description());
        metadata.put(EXTERNALIZED_KEY, field.externalized());
        metadata.put(ITEMS_KEY, itemMetadata);
        metadata.put(MIN_ITEMS_KEY, field.minItems());
        metadata.put(MAX_ITEMS_KEY, field.maxItems());
        metadata.put(UNIQUE_ITEMS_KEY, field.uniqueItems());
        metadata.put(CONTAINS_KEY, field.contains());
        metadata.put(USE_SUB_OBJECT_DEFAULT_KEY, field.useSubObjectDefault());
        metadata.put(DEFAULT_VALUE_KEY, field.defaultValue());
        return metadata;
    }

    private static LinkedHashMap<String, Object> parseMapMetadata(final MapField field, final ProcessingEnvironment processingEnvironment) {
        String canonicalName;
        try {
            canonicalName = field.valueType().getCanonicalName();
        } catch (MirroredTypeException e) {
            canonicalName = e.getTypeMirrors().get(0).toString();
        }
        final var valueElement = AnnotationUtils.safeGetElement(canonicalName, processingEnvironment);
        final var valueMetadata = new LinkedHashMap<String, Object>();
        gatherObjectSchemaData(valueElement, valueMetadata, processingEnvironment);
        final var metadata = new LinkedHashMap<String, Object>();
        metadata.put(TYPE_KEY, MAP_TYPE);
        metadata.put(ID_KEY, getUUID());
        metadata.put(EXTERNALIZED_KEY_NAME, field.externalizedKeyName());
        metadata.put(CONFIG_FIELD_NAME_KEY, field.configFieldName());
        metadata.put(DESCRIPTION_KEY, field.description());
        metadata.put(EXTERNALIZED_KEY, field.externalized());
        metadata.put(ADDITIONAL_PROPERTIES_KEY, valueMetadata);
        metadata.put(DEFAULT_VALUE_KEY, field.defaultValue());
        return metadata;
    }

    /**
     * Parses the 'IntegerField' annotation and returns a LinkedHashMap containing the metadata.
     * Review the IntegerField interface to view all available fields.
     * @param field The field of the config to parse.
     * @return A LinkedHashMap containing the metadata.
     */
    private static LinkedHashMap<String, Object> parseIntegerMetadata(final IntegerField field) {
        final var metadata = new LinkedHashMap<String, Object>();
        metadata.put(TYPE_KEY, INTEGER_TYPE);
        metadata.put(ID_KEY, getUUID());
        metadata.put(EXTERNALIZED_KEY_NAME, field.externalizedKeyName());
        metadata.put(CONFIG_FIELD_NAME_KEY, field.configFieldName());
        metadata.put(DESCRIPTION_KEY, field.description());
        metadata.put(EXTERNALIZED_KEY, field.externalized());
        metadata.put(DEFAULT_VALUE_KEY, field.defaultValue());
        metadata.put(MINIMUM_KEY, field.min());
        metadata.put(MAXIMUM_KEY, field.max());
        metadata.put(EXCLUSIVE_MIN_KEY, field.exclusiveMin());
        metadata.put(EXCLUSIVE_MAX_KEY, field.exclusiveMax());
        metadata.put(MULTIPLE_OF_KEY, field.multipleOf());
        metadata.put(FORMAT_KEY, field.format().name());
        return metadata;
    }

    /**
     * Parses the 'NumberField' annotation and returns a LinkedHashMap containing the metadata.
     * Review the NumberField interface to view all available fields.
     * @param field The field of the config to parse.
     * @return A LinkedHashMap containing the metadata.
     */
    private static LinkedHashMap<String, Object> parseNumberMetadata(final NumberField field) {
        final var metadata = new LinkedHashMap<String, Object>();
        metadata.put(TYPE_KEY, NUMBER_TYPE);
        metadata.put(ID_KEY, getUUID());
        metadata.put(EXTERNALIZED_KEY_NAME, field.externalizedKeyName());
        metadata.put(CONFIG_FIELD_NAME_KEY, field.configFieldName());
        metadata.put(DESCRIPTION_KEY, field.description());
        metadata.put(EXTERNALIZED_KEY, field.externalized());
        metadata.put(DEFAULT_VALUE_KEY, field.defaultValue());
        metadata.put(MINIMUM_KEY, field.min());
        metadata.put(MAXIMUM_KEY, field.max());
        metadata.put(EXCLUSIVE_MIN_KEY, field.exclusiveMin());
        metadata.put(EXCLUSIVE_MAX_KEY, field.exclusiveMax());
        metadata.put(MULTIPLE_OF_KEY, field.multipleOf());
        metadata.put(FORMAT_KEY, field.format().name());
        return metadata;
    }

    /**
     * Parses the 'StringField' annotation and returns a LinkedHashMap containing the metadata.
     * Review the StringField interface to view all available fields.
     * @param field The field of the config to parse.
     * @return A LinkedHashMap containing the metadata.
     */
    private static LinkedHashMap<String, Object> parseStringMetadata(final StringField field) {
        final var metadata = new LinkedHashMap<String, Object>();
        metadata.put(TYPE_KEY, STRING_TYPE);
        metadata.put(ID_KEY, getUUID());
        metadata.put(EXTERNALIZED_KEY_NAME, field.externalizedKeyName());
        metadata.put(CONFIG_FIELD_NAME_KEY, field.configFieldName());
        metadata.put(DESCRIPTION_KEY, field.description());
        metadata.put(EXTERNALIZED_KEY, field.externalized());
        metadata.put(DEFAULT_VALUE_KEY, field.defaultValue());
        metadata.put(MIN_LENGTH_KEY, field.minLength());
        metadata.put(MAX_LENGTH_KEY, field.maxLength());
        metadata.put(PATTERN_KEY, field.pattern());
        metadata.put(FORMAT_KEY, field.format().name());
        return metadata;
    }

    /**
     * Parses the 'ObjectField' annotation and returns a LinkedHashMap containing the metadata.
     * Review the ObjectField interface to view all available fields.
     * @param field The field of the config to parse.
     * @param processingEnvironment The processing environment to resolve subtypes.
     * @return A LinkedHashMap containing the metadata.
     */
    private static LinkedHashMap<String, Object> parseObjectMetadata(final ObjectField field, final ProcessingEnvironment processingEnvironment) {
        String canonicalName;
        try {
            canonicalName = field.ref().getCanonicalName();
        } catch (MirroredTypeException e) {
            canonicalName = e.getTypeMirrors().get(0).toString();
        }
        final var refElement = AnnotationUtils.safeGetElement(canonicalName, processingEnvironment);
        final var refMetadata = new LinkedHashMap<String, Object>();
        gatherObjectSchemaData(refElement, refMetadata, processingEnvironment);
        final var metadata = new LinkedHashMap<String, Object>();
        metadata.put(TYPE_KEY, OBJECT_TYPE);
        metadata.put(ID_KEY, getUUID());
        metadata.put(EXTERNALIZED_KEY_NAME, field.externalizedKeyName());
        metadata.put(CONFIG_FIELD_NAME_KEY, field.configFieldName());
        metadata.put(DESCRIPTION_KEY, field.description());
        metadata.put(EXTERNALIZED_KEY, field.externalized());
        metadata.put(USE_SUB_OBJECT_DEFAULT_KEY, field.useSubObjectDefault());
        metadata.put(DEFAULT_VALUE_KEY, field.defaultValue());
        metadata.put(REF_KEY, refMetadata);
        return metadata;
    }

    /**
     * Parses the 'NullField' annotation and returns a LinkedHashMap containing the metadata.
     * Review the NullField interface to view all available fields.
     * @param field The field of the config to parse.
     * @return A LinkedHashMap containing the metadata.
     */
    private static LinkedHashMap<String, Object> parseNullMetadata(final NullField field) {
        final var metadata = new LinkedHashMap<String, Object>();
        metadata.put(TYPE_KEY, NULL_TYPE);
        metadata.put(ID_KEY, getUUID());
        metadata.put(EXTERNALIZED_KEY_NAME, field.externalizedKeyName());
        metadata.put(CONFIG_FIELD_NAME_KEY, field.configFieldName());
        metadata.put(DESCRIPTION_KEY, field.description());
        metadata.put(EXTERNALIZED_KEY, field.externalized());
        metadata.put(DEFAULT_VALUE_KEY, field.defaultValue());
        return metadata;
    }

    /**
     * Parses the 'BooleanField' annotation and returns a LinkedHashMap containing the metadata.
     * Review the BooleanField interface to view all available fields.
     * @param field The field of the config to parse.
     * @return A LinkedHashMap containing the metadata.
     */
    private static LinkedHashMap<String, Object> parseBooleanMetadata(final BooleanField field) {
        final var metadata = new LinkedHashMap<String, Object>();
        metadata.put(TYPE_KEY, BOOLEAN_TYPE);
        metadata.put(ID_KEY, getUUID());
        metadata.put(EXTERNALIZED_KEY_NAME, field.externalizedKeyName());
        metadata.put(CONFIG_FIELD_NAME_KEY, field.configFieldName());
        metadata.put(DESCRIPTION_KEY, field.description());
        metadata.put(EXTERNALIZED_KEY, field.externalized());
        metadata.put(DEFAULT_VALUE_KEY, field.defaultValue());
        return metadata;
    }

    private static String getUUID() {
        final var id = UUID.randomUUID();
        return id.toString();
    }
}
