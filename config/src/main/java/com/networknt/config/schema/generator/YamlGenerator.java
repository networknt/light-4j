package com.networknt.config.schema.generator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.schema.AnnotationUtils;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.MetadataParser;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.comments.CommentLine;
import org.yaml.snakeyaml.comments.CommentType;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.events.CommentEvent;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.representer.Representer;

import javax.tools.FileObject;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Generates Light4J style yaml configuration files.
 *
 * @author Kalev Gonvick
 */
public class YamlGenerator extends Generator {

    private static final String EXTERNAL_CONFIG_PREFIX = "${";
    private static final String EXTERNAL_CONFIG_SUFFIX = "}";
    private static final DumperOptions YAML_OPTIONS = new DumperOptions();
    static {
        YAML_OPTIONS.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        YAML_OPTIONS.setIndent(2);
        YAML_OPTIONS.setPrettyFlow(true);
        YAML_OPTIONS.setProcessComments(true);
        YAML_OPTIONS.setSplitLines(false);
    }

    public YamlGenerator(final String configKey, final String configName) {
        super(configKey, configName);
    }

    @Override
    protected LinkedHashMap<String, Object> getRootSchemaProperties(LinkedHashMap<String, Object> metadata) {
        final var properties = new LinkedHashMap<String, Object>();
        final var metadataProperties = (LinkedHashMap<String, Object>) metadata.get(MetadataParser.PROPERTIES_KEY);

        metadataProperties.forEach((key, value) -> {
            final var property = new LinkedHashMap<String, Object>();
            if (!(value instanceof LinkedHashMap))
                return;
            this.parseField((LinkedHashMap<String, Object>) value, property);
            properties.putAll(property);
        });

        return properties;
    }

    @Override
    public void writeSchemaToFile(final FileObject object, final LinkedHashMap<String, Object> metadata) throws IOException {
        writeSchemaToFile(object.openOutputStream(), metadata);
    }

    @Override
    public void writeSchemaToFile(final OutputStream os, final LinkedHashMap<String, Object> metadata) throws IOException {
        final var json = new LinkedHashMap<>(this.getRootSchemaProperties(metadata));
        final var rootDescription = AnnotationUtils.getAsType(metadata.get(MetadataParser.DESCRIPTION_KEY), String.class);
        final var yaml = new Yaml(new YamlCommentRepresenter(YAML_OPTIONS, metadata, rootDescription), YAML_OPTIONS);
        final var fileContent = yaml.dump(json);
        os.write(fileContent.getBytes());
    }

    @Override
    public void writeSchemaToFile(final Writer writer, final LinkedHashMap<String, Object> metadata) throws IOException {
        final var json = new LinkedHashMap<>(this.getRootSchemaProperties(metadata));
        final var rootDescription = AnnotationUtils.getAsType(metadata.get(MetadataParser.DESCRIPTION_KEY), String.class);
        final var yaml = new Yaml(new YamlCommentRepresenter(YAML_OPTIONS, metadata, rootDescription), YAML_OPTIONS);
        yaml.dump(json, writer);
    }

    /**
     * Builds the yaml property value for Light4J configurations.
     * If externalized, the property value will be formatted as ${configFileName.configFieldName:defaultValue}
     * The default value is only added if it was set in the Annotation.
     *
     * @param field The field to parse.
     * @param property The property to add the field to.
     */
    private void buildYamlProperty(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property) {
        final var externalized = AnnotationUtils.getAsType(field.get(MetadataParser.EXTERNALIZED_KEY), Boolean.class);
        final var isExternalized = externalized != null && externalized;
        final var externalizedKeyName = AnnotationUtils.getAsType(field.get(MetadataParser.EXTERNALIZED_KEY_NAME), String.class);
        final var uuid = AnnotationUtils.getAsType(field.get(MetadataParser.ID_KEY), String.class);
        final var configFieldName = AnnotationUtils.getAsType(field.get(MetadataParser.CONFIG_FIELD_NAME_KEY), String.class);
        final var defaultValue = field.get(MetadataParser.DEFAULT_VALUE_KEY);

        /* Don't stringify non-string values if not externalized. */
        if (!(defaultValue instanceof String) && defaultValue != null && !isExternalized) {
            property.put(configFieldName, defaultValue);
            return;
        }

        final var builder = new StringBuilder();
        if (isExternalized)
            builder.append(EXTERNAL_CONFIG_PREFIX)
                    .append(this.configKey)
                    .append(".")
                    .append(externalizedKeyName)
                    .append(":");

        if (isExternalized && defaultValue != null)
            builder.append(defaultValue);

        else if (defaultValue != null) {
            final var mapper = Config.getInstance().getMapper();
            final var stringValue = (String) defaultValue;

            try {
                final var jsonMapValue = mapper.readValue(stringValue, new TypeReference<LinkedHashMap<String, Object>>() {});
                property.put(configFieldName + YamlCommentRepresenter.REPRESENTER_SEPARATOR + uuid, jsonMapValue);
                return;

            } catch (Exception me) {

                try {
                    final var jsonListValue = mapper.readValue(stringValue, new TypeReference<List<String>>() {});
                    property.put(configFieldName + YamlCommentRepresenter.REPRESENTER_SEPARATOR + uuid, jsonListValue);
                    return;

                } catch (Exception le) {
                    // do nothing
                }
            }
        }

        if (isExternalized)
            builder.append(EXTERNAL_CONFIG_SUFFIX);

        final var propertyValue = builder.toString();
        property.put(configFieldName + YamlCommentRepresenter.REPRESENTER_SEPARATOR + uuid, propertyValue);
    }

    @Override
    protected void parseArray(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property) {

        final var useSubObjectDefault = AnnotationUtils.getAsType(field.get(MetadataParser.USE_SUB_OBJECT_DEFAULT_KEY), Boolean.class);
        if (useSubObjectDefault != ConfigSchema.DEFAULT_BOOLEAN) {

            if (Generator.fieldIsSubMap(field, MetadataParser.ITEMS_KEY)) {
                final var props = (LinkedHashMap<String, Object>) field.get(MetadataParser.ITEMS_KEY);
                props.values().forEach(value -> {
                    final var itemProp = new LinkedHashMap<String, Object>();
                    this.parseField((LinkedHashMap<String, Object>) value, itemProp);
                    property.putAll(itemProp);
                });
            }
        } else this.buildYamlProperty(field, property);

    }

    @Override
    protected void parseMapField(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property) {
        this.buildYamlProperty(field, property);
    }

    @Override
    protected void parseBoolean(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property) {
        this.buildYamlProperty(field, property);
    }

    @Override
    protected void parseInteger(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property) {
        this.buildYamlProperty(field, property);
    }

    @Override
    protected void parseNumber(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property) {
        this.buildYamlProperty(field, property);
    }

    @Override
    protected void parseObject(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property) {

        final var useSubObjectDefault = AnnotationUtils.getAsType(field.get(MetadataParser.USE_SUB_OBJECT_DEFAULT_KEY), Boolean.class);
        if (useSubObjectDefault != null && useSubObjectDefault != ConfigSchema.DEFAULT_BOOLEAN) {

            final LinkedHashMap<String, Object> props;
            if (Generator.fieldIsSubMap(field, MetadataParser.PROPERTIES_KEY))
                props = (LinkedHashMap<String, Object>) field.get(MetadataParser.PROPERTIES_KEY);

            else if (Generator.fieldIsSubMap(field, MetadataParser.REF_KEY)) {
                final var refProps = (LinkedHashMap<String, Object>) field.get(MetadataParser.REF_KEY);
                props = (LinkedHashMap<String, Object>) refProps.get(MetadataParser.PROPERTIES_KEY);
            }

            else if (Generator.fieldIsSubMap(field, MetadataParser.ADDITIONAL_PROPERTIES_KEY)) {
                final var additionalProps = (LinkedHashMap<String, Object>) field.get(MetadataParser.ADDITIONAL_PROPERTIES_KEY);
                props = (LinkedHashMap<String, Object>) additionalProps.get(MetadataParser.PROPERTIES_KEY);
            }

            else props = new LinkedHashMap<>();


            final var objectProperties = new LinkedHashMap<String, Object>();
            props.forEach((key, value) -> {
                this.parseField((LinkedHashMap<String, Object>) value, objectProperties);
            });

            final var configFieldName = AnnotationUtils.getAsType(field.get(MetadataParser.CONFIG_FIELD_NAME_KEY), String.class);
            final var uuid = AnnotationUtils.getAsType(field.get(MetadataParser.ID_KEY), String.class);
            property.put(configFieldName + YamlCommentRepresenter.REPRESENTER_SEPARATOR + uuid, objectProperties);


        } else this.buildYamlProperty(field, property);

    }

    @Override
    protected void parseString(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property) {
        this.buildYamlProperty(field, property);
    }

    @Override
    protected void parseNullField(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property) {
        this.buildYamlProperty(field, property);
    }



    /**
     * Custom representer to add comments to the yaml schema.
     */
    public static class YamlCommentRepresenter extends Representer {

        public static final String REPRESENTER_SEPARATOR = "___";
        private final AtomicBoolean firstNodeProcessed = new AtomicBoolean(false);

        public YamlCommentRepresenter(final DumperOptions options, final LinkedHashMap<String, Object> metadata, final String rootDescription) {
            super(options);
            this.representers.put(String.class, new RepresentString() {

                @Override
                public Node representData(Object data) {
                    final String configFieldName;
                    final String uuid;

                    final var stringData = (String) data;
                    if (stringData.contains(REPRESENTER_SEPARATOR)) {
                        final var parts = stringData.split(REPRESENTER_SEPARATOR);
                        configFieldName = parts[0];
                        uuid = parts[1];
                    } else {
                        configFieldName = stringData;
                        uuid = null;
                    }
                    Node node = super.representData(configFieldName);
                    final var start = node.getStartMark();
                    final var end = node.getEndMark();
                    final var descriptionBuilder = new StringBuilder();

                    final var nodeDescription = findCommentForNode(metadata, configFieldName, uuid);

                    if (!firstNodeProcessed.compareAndExchange(false, true) && rootDescription != null) {
                        descriptionBuilder.append(rootDescription);
                        descriptionBuilder.append('\n');
                    }

                    if (nodeDescription != null && !nodeDescription.isEmpty()) {
                        descriptionBuilder.append(nodeDescription);
                    }
//                    final var description = findCommentForNode(metadata, configFieldName, uuid);

                    final var description = descriptionBuilder.toString();
                    if (!description.isEmpty())
                        this.addCommentsToNode(start, end, description, node);

                    return node;
                }

                /**
                 * Adds comments to the node. If the description contains multiple lines, each line will be added as a separate comment.
                 * @param start - The start mark of the node.
                 * @param end - The end mark of the node.
                 * @param description - The description to add as a comment.
                 * @param node - The node to add the comments to.
                 */
                private void addCommentsToNode(final Mark start, final Mark end, final String description, final Node node) {
                    final var commentLines = new ArrayList<CommentLine>();

                    /* Add description comment line or multiple lines if the description contains multiple lines. */
                    if (description.contains("\n")) {
                        final var lines = description.split("\n");

                        for (final var line : lines)
                            commentLines.add(new CommentLine(new CommentEvent(CommentType.BLOCK, " " + line, start, end)));

                    } else commentLines.add(new CommentLine(new CommentEvent(CommentType.BLOCK, " " + description, start, end)));

                    node.setBlockComments(commentLines);
                }

                /**
                 * Recursive function to search for the description of the current node based on the name and id.
                 * @param metadata - The metadata to search through.
                 * @param nodeName - The name of the node to search for.
                 * @param uuid - The id of the node to search for.
                 * @return The description of the node if found, otherwise null.
                 */
                private String findCommentForNode(final LinkedHashMap<String, Object> metadata, final String nodeName, final String uuid) {
                    String returnString = null;
                    for (final var entry : metadata.entrySet()) {

                        if (!(entry.getValue() instanceof LinkedHashMap))
                            continue;

                        final var field = (LinkedHashMap<String, Object>) entry.getValue();
                        final var fieldId = field.get(MetadataParser.ID_KEY);
                        if (fieldId != null && entry.getKey().equals(nodeName) && fieldId.equals(uuid)) {
                            returnString =  (String) field.get(MetadataParser.DESCRIPTION_KEY);
                            break;

                        } else {
                            returnString = findCommentForNode(field, nodeName, uuid);
                            if (returnString != null)
                                break;
                        }
                    }
                    return returnString;
                }

            });
        }
    }



}
