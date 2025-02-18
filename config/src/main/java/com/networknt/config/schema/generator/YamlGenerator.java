package com.networknt.config.schema.generator;

import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.MetadataParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.comments.CommentLine;
import org.yaml.snakeyaml.comments.CommentType;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.events.CommentEvent;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.representer.Representer;

import javax.tools.FileObject;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Generates Light4J style yaml configuration files.
 *
 * @author Kalev Gonvick
 */
public class YamlGenerator extends Generator {

    private static final Logger LOG = LoggerFactory.getLogger(YamlGenerator.class);
    private static final DumperOptions YAML_OPTIONS = new DumperOptions();
    static {
        YAML_OPTIONS.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        YAML_OPTIONS.setIndent(2);
        YAML_OPTIONS.setPrettyFlow(true);
        YAML_OPTIONS.setProcessComments(true);
        YAML_OPTIONS.setSplitLines(false);
    }

    public YamlGenerator(final String configKey) {
        super(configKey);
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
    public void writeSchemaToFile(final OutputStream os, final LinkedHashMap<String, Object> metadata) {
        try {
            final var json = new LinkedHashMap<>(this.getRootSchemaProperties(metadata));
            final var yaml = new Yaml(new YamlCommentRepresenter(YAML_OPTIONS, metadata), YAML_OPTIONS);
            final var fileContent = yaml.dump(json);
            os.write(fileContent.getBytes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void writeSchemaToFile(final Writer writer, final LinkedHashMap<String, Object> metadata) throws IOException {
        try {
            final var json = new LinkedHashMap<>(this.getRootSchemaProperties(metadata));
            final var yaml = new Yaml(new YamlCommentRepresenter(YAML_OPTIONS, metadata), YAML_OPTIONS);
            yaml.dump(json, writer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        final var externalized = this.getAsType(field.get(MetadataParser.EXTERNALIZED_KEY), Boolean.class);
        final var isExternalized = externalized != null && externalized;
        final var configFieldName = this.getAsType(field.get(MetadataParser.CONFIG_FIELD_NAME_KEY), String.class);
        final var defaultValue = field.get(MetadataParser.DEFAULT_VALUE_KEY);

        /* Don't stringify non-string values if not externalized. */
        if (!(defaultValue instanceof String) && defaultValue != null && !isExternalized) {
            property.put(configFieldName, defaultValue);
            return;
        }

        final var builder = new StringBuilder();
        if (isExternalized)
            builder.append("${").append(this.configKey).append(".").append(configFieldName).append(":");

        if (defaultValue != null)
            builder.append(defaultValue);

        if (isExternalized)
            builder.append("}");

        final var propertyValue = builder.toString();
        property.put(configFieldName, propertyValue);
    }

    @Override
    protected void parseArray(final LinkedHashMap<String, Object> field, final LinkedHashMap<String, Object> property) {

        final var useSubObjectDefault = this.getAsType(field.get(MetadataParser.USE_SUB_OBJECT_DEFAULT_KEY), Boolean.class);
        if (useSubObjectDefault != ConfigSchema.DEFAULT_BOOLEAN) {

            if (Generator.fieldIsHashMap(field, MetadataParser.ITEMS_KEY)) {

                // TODO - test this with items
                final var props = (LinkedHashMap<String, Object>) field.get(MetadataParser.ITEMS_KEY);
                props.values().forEach(value -> {
                    final var itemProp = new LinkedHashMap<String, Object>();
                    this.parseField((LinkedHashMap<String, Object>) value, itemProp);
                    property.putAll(itemProp);
                });
            } else {
                // TODO - handle itemsAllOf, itemsOneOf, itemsAnyOf
                throw new IllegalStateException("Not implemented yet.");
            }
        } else {
            this.buildYamlProperty(field, property);
        }
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

        final var useSubObjectDefault = this.getAsType(field.get(MetadataParser.USE_SUB_OBJECT_DEFAULT_KEY), Boolean.class);
        if (useSubObjectDefault != ConfigSchema.DEFAULT_BOOLEAN) {

            if (Generator.fieldIsHashMap(field, MetadataParser.PROPERTIES_KEY)) {

                // TODO - test this with properties
                final var props = (LinkedHashMap<String, Object>) field.get(MetadataParser.PROPERTIES_KEY);
                props.values().forEach(value -> {
                    final var objectProp = new LinkedHashMap<String, Object>();
                    this.parseField((LinkedHashMap<String, Object>) value, objectProp);
                    property.putAll(objectProp);
                });

            } else {
                // TODO - handle allOf, oneOf, anyOf
                throw new IllegalStateException("Not implemented yet.");
            }

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


        public YamlCommentRepresenter(final DumperOptions options, final LinkedHashMap<String, Object> metadata) {
            super(options);
            this.representers.put(String.class, new RepresentString() {

                final LinkedHashMap<String, Object> innerMetadata = metadata;

                @Override
                public Node representData(Object data) {
                    Node node = super.representData(data);
                    final var start = node.getStartMark();
                    final var end = node.getEndMark();

                    if (Generator.fieldIsHashMap(innerMetadata, MetadataParser.PROPERTIES_KEY)) {

                        final var allProperties = (LinkedHashMap<String, Object>) this.innerMetadata.get(MetadataParser.PROPERTIES_KEY);
                        final var currentYamlField = (String) data;

                        if (Generator.fieldIsHashMap(allProperties, currentYamlField)) {
                            final var yamlFieldProp = (LinkedHashMap<String, Object>) allProperties.get(currentYamlField);
                            final var description = (String) yamlFieldProp.get(MetadataParser.DESCRIPTION_KEY);

                            if (!description.isEmpty())
                                this.addCommentsToNode(start, end, description, node);

                        }
                    }

                    return node;
                }

                private void addCommentsToNode(final Mark start, final Mark end, final String description, final Node node) {
                    final var commentLines = new ArrayList<CommentLine>();

                    /* Add description comment line or multiple lines if the description contains multiple lines. */
                    if (description.contains("\n")) {
                        final var lines = description.split("\n");

                        for (final var line : lines)
                            commentLines.add(new CommentLine(new CommentEvent(CommentType.BLOCK, line, start, end)));


                    } else commentLines.add(new CommentLine(new CommentEvent(CommentType.BLOCK, description, start, end)));

                    node.setBlockComments(commentLines);
                }

            });
        }
    }

}

