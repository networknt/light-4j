package com.networknt.config.schema.generator;

import com.networknt.config.schema.FieldNode;
import com.networknt.config.schema.FieldType;
import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.comments.CommentLine;
import org.snakeyaml.engine.v2.comments.CommentType;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.exceptions.Mark;
import org.snakeyaml.engine.v2.events.CommentEvent;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.representer.StandardRepresenter;
import org.snakeyaml.engine.v2.schema.CoreSchema;

import javax.tools.FileObject;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Generates Light4J style yaml configuration files.
 *
 * @author Kalev Gonvick
 */
public class YamlGenerator extends Generator {

    private static final String EXTERNAL_CONFIG_PREFIX = "${";
    private static final String EXTERNAL_CONFIG_SUFFIX = "}";
    public static final String EXAMPLE_HEADER = "Example value (replace the field placeholder; no outer wrapper):";
    public static final String FIELD_REFERENCE_HEADER = "Value fields (required/optional, defaults and constraints):";
    protected static final DumpSettings YAML_OPTIONS = DumpSettings.builder()
            .setSchema(new CoreSchema())
            .setDefaultFlowStyle(FlowStyle.BLOCK)
            .setIndent(2)
            .setMultiLineFlow(true)
            .setDumpComments(true)
            .setSplitLines(false)
            .build();

    public YamlGenerator(final String configKey, final String configName) {
        super(configKey, configName);
    }

    @Override
    protected LinkedHashMap<String, Object> convertConfigRoot(FieldNode annotatedField) {
        final var properties = new LinkedHashMap<String, Object>();
        annotatedField.getChildren()
                .filter(nodes -> !nodes.isEmpty())
                .ifPresent(nodes -> nodes.stream()
                        .map(this::convertNode)
                        .forEach(properties::putAll));
        return properties;
    }

    @Override
    public void writeSchemaToFile(final FileObject object, final FieldNode annotatedField) throws IOException {
        writeSchemaToFile(object.openOutputStream(), annotatedField);
    }

    @Override
    public void writeSchemaToFile(final OutputStream os, final FieldNode annotatedField) throws IOException {
        final var json = this.convertConfigRoot(annotatedField);
        final var yaml = new Dump(YAML_OPTIONS, new YamlCommentRepresenter(YAML_OPTIONS, annotatedField));
        final var fileContent = yaml.dumpToString(json);
        os.write(fileContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Override
    public void writeSchemaToFile(final Writer writer, final FieldNode annotatedField) {
        final var json = this.convertConfigRoot(annotatedField);
        final var yaml = new Dump(YAML_OPTIONS, new YamlCommentRepresenter(YAML_OPTIONS, annotatedField));
        try {
            writer.write(yaml.dumpToString(json));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    protected LinkedHashMap<String, Object> convertArrayNode(final FieldNode annotatedField) {
        return convertNested(annotatedField);
    }

    @Override
    protected LinkedHashMap<String, Object> convertMapNode(final FieldNode annotatedField) {
        return convertNested(annotatedField);
    }

    private LinkedHashMap<String, Object> convertNested(final FieldNode annotatedField) {
        if (annotatedField.getUseSubTypeDefault().isEmpty()) {
            return this.buildYamlProperty(annotatedField);
        }
        return this.buildChildYamlProperties(annotatedField)
                .or(() -> annotatedField.getRef().map(this::convertNode))
                .orElse(new LinkedHashMap<>());
    }

    @Override
    protected LinkedHashMap<String, Object> convertBooleanNode(final FieldNode annotatedField) {
        return this.buildYamlProperty(annotatedField);
    }

    @Override
    protected LinkedHashMap<String, Object> convertIntegerNode(final FieldNode annotatedField) {
        return this.buildYamlProperty(annotatedField);
    }

    @Override
    protected LinkedHashMap<String, Object> convertNumberNode(final FieldNode annotatedField) {
        return this.buildYamlProperty(annotatedField);
    }

    @Override
    protected LinkedHashMap<String, Object> convertObjectNode(final FieldNode annotatedField) {
        final var key = annotatedField.getConfigFieldName() + YamlCommentRepresenter.REPRESENTER_SEPARATOR + annotatedField.getId().toString();
        if (annotatedField.getUseSubTypeDefault().isEmpty()) {
            return this.buildYamlProperty(annotatedField);
        }
        return this.buildChildYamlProperties(annotatedField).map(props -> this.wrapWithKey(key, props))
                .or(() -> annotatedField.getRef().flatMap(this::buildChildYamlProperties)
                        .map(props -> this.wrapWithKey(key, props)))
                .or(() -> annotatedField.getAnyOf().flatMap(this::buildMultiClassYamlProperty)
                        .map(props -> this.wrapWithKey(key, props)))
                .or(() -> annotatedField.getAllOf().flatMap(this::buildMultiClassYamlProperty)
                        .map(props -> this.wrapWithKey(key, props)))
                .or(() -> annotatedField.getOneOf().flatMap(this::buildMultiClassYamlProperty)
                        .map(props -> wrapWithKey(key, props)))
                .orElseGet(() -> this.buildYamlProperty(annotatedField));
    }

    @Override
    protected LinkedHashMap<String, Object> convertStringNode(final FieldNode annotatedField) {
        return this.buildYamlProperty(annotatedField);
    }

    @Override
    protected LinkedHashMap<String, Object> convertNullNode(final FieldNode property) {
        return this.buildYamlProperty(property);
    }

    /**
     * For any multi list of nodes, take the first node and format it into a yaml property.
     * You can't represent anyOf, oneOf, allOf, etc. in a yaml configuration.
     *
     * @param nodes - List of nodes with potential child nodes.
     * @return - Returns a JSON schema formatted hashmap.
     */
    private Optional<LinkedHashMap<String, Object>> buildMultiClassYamlProperty(final List<FieldNode> nodes) {
        return nodes.stream().findFirst().flatMap(FieldNode::getChildren).map(childNodes -> {
            final var inner = new LinkedHashMap<String, Object>();
            childNodes.stream().map(this::convertNode).forEach(inner::putAll);
            return inner;
        });
    }

    private Optional<LinkedHashMap<String, Object>> buildChildYamlProperties(final FieldNode node) {
        return node.getChildren().map(nodes -> {
            final var inner = new LinkedHashMap<String, Object>();
            nodes.stream().map(this::convertNode).forEach(inner::putAll);
            return inner;
        });
    }

    private LinkedHashMap<String, Object> wrapWithKey(final String key, final LinkedHashMap<String, Object> inner) {
        var outer = new LinkedHashMap<String, Object>();
        outer.put(key, inner);
        return outer;
    }

    /**
     * Builds the yaml property value for Light4J configurations.
     * If externalized, the property value will be formatted as ${configFileName.configFieldName:defaultValue}
     * The default value is only added if it was set in the Annotation.
     *
     * @param annotatedField Contains all the data parsed from our annotated config field.
     */
    protected LinkedHashMap<String, Object> buildYamlProperty(final FieldNode annotatedField) {
        var yamlProp = new LinkedHashMap<String, Object>();
        final boolean externalized = annotatedField.getExternalizedKeyName().isPresent();
        final var externalName = annotatedField.getExternalizedKeyName();
        final var id = annotatedField.getId().toString();
        final var configFieldName = annotatedField.getConfigFieldName();
        final var defaultValue = annotatedField.getDefaultValue();

        final var injection = annotatedField.getInjection().orElse(true);

        /* Don't stringify non-string values if not externalized. */
        if ((defaultValue.isPresent() && !externalized) || !injection) {
            yamlProp.put(configFieldName, defaultValue.get());
            return yamlProp;
        }
        final var builder = new StringBuilder();
        externalName.ifPresent(s -> builder.append(EXTERNAL_CONFIG_PREFIX)
                .append(this.configKey)
                .append(".")
                .append(s)
                .append(":"));
        defaultValue.ifPresent(builder::append);
        externalName.ifPresent(s -> builder.append(EXTERNAL_CONFIG_SUFFIX));
        final var propertyValue = builder.toString();
        final var propertyName = configFieldName + YamlCommentRepresenter.REPRESENTER_SEPARATOR + id;
        yamlProp.put(propertyName, propertyValue);
        return yamlProp;
    }


    /**
     * Custom representer to add comments to the yaml schema.
     */
    public static class YamlCommentRepresenter extends StandardRepresenter {
        private static final char LINE_SEPARATOR = '\n';
        public static final String REPRESENTER_SEPARATOR = "___";
        private final Dump exampleYaml;

        private final AtomicBoolean firstNodeProcessed = new AtomicBoolean(false);

        private static void appendFieldReference(StringBuilder text, FieldNode node, String prefix) {
            node.getChildren().ifPresent(children -> children.forEach(child -> {
                String path = prefix + child.getConfigFieldName();
                child.getDescription().ifPresent(description ->
                        text.append(path).append(": ").append(description).append('\n'));
                if (child.getType() == FieldType.OBJECT) {
                    appendFieldReference(text, child.getRef().orElse(child), path + ".");
                }
            }));
        }

        public YamlCommentRepresenter(final DumpSettings options, final FieldNode rootAnnotatedField) {
            super(options);
            var exampleOptions = DumpSettings.builder().setSchema(new CoreSchema())
                    .setDefaultFlowStyle(FlowStyle.BLOCK).setIndent(2).build();
            exampleYaml = new Dump(exampleOptions);
            this.representers.put(String.class, new CommentedStringRepresenter(rootAnnotatedField));
        }

        private final class CommentedStringRepresenter extends RepresentString {
            private final FieldNode rootAnnotatedField;

            private CommentedStringRepresenter(FieldNode rootAnnotatedField) {
                this.rootAnnotatedField = rootAnnotatedField;
            }

            @Override
            public Node representData(Object nodeData) {
                final String nodeConfigName;
                final String nodeId;

                final var stringData = (String) nodeData;
                if (stringData.contains(REPRESENTER_SEPARATOR)) {
                    final var parts = stringData.split(REPRESENTER_SEPARATOR);
                    nodeConfigName = parts[0];
                    nodeId = parts[1];
                } else {
                    nodeConfigName = stringData;
                    nodeId = null;
                }
                final var node = super.representData(nodeConfigName);
                final var start = node.getStartMark();
                final var end = node.getEndMark();
                final var descriptionBuilder = new StringBuilder();
                final var nodeDescription = findCommentForNode(rootAnnotatedField, nodeConfigName, nodeId);
                if (!firstNodeProcessed.compareAndExchange(false, true)) {
                    rootAnnotatedField.getDescription().ifPresent(description -> {
                        descriptionBuilder.append(description);
                        descriptionBuilder.append(LINE_SEPARATOR);
                    });
                }
                nodeDescription.ifPresent(descriptionBuilder::append);
                final var description = descriptionBuilder.toString();
                if (!description.isBlank())
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
            private void addCommentsToNode(final Optional<Mark> start, final Optional<Mark> end, final String description, final Node node) {
                final var commentLines = new ArrayList<CommentLine>();

                // Split the description by newline and add comments to the node.
                Arrays.stream(description.split(String.valueOf(LINE_SEPARATOR))).forEach(line -> {
                    final String spacedLine;
                    if (line.isBlank())
                        spacedLine = line;
                    else spacedLine = " " + line;
                    commentLines.add(new CommentLine(new CommentEvent(CommentType.BLOCK, spacedLine, start, end)));
                });
                node.setBlockComments(commentLines);
            }

            /**
             * Recursive function to search for the description of the current node based on the name and id.
             * @param annotatedField - The metadata to search through.
             * @param nodeName - The name of the node to search for.
             * @param uuid - The id of the node to search for.
             * @return The description of the node if found, otherwise null.
             */
            private Optional<String> findCommentForNode(final FieldNode annotatedField, final String nodeName, final String uuid) {
                if (annotatedField.getConfigFieldName().equals(nodeName) && annotatedField.getId().toString().equals(uuid))
                    return describeField(annotatedField);

                var children = new ArrayList<FieldNode>();
                annotatedField.getRef().ifPresent(children::add);
                annotatedField.getChildren().ifPresent(children::addAll);
                annotatedField.getAnyOf().ifPresent(children::addAll);
                annotatedField.getAllOf().ifPresent(children::addAll);
                annotatedField.getOneOf().ifPresent(children::addAll);
                return children.stream()
                        .map(child -> findCommentForNode(child, nodeName, uuid))
                        .flatMap(Optional::stream)
                        .findFirst();
            }

            private Optional<String> describeField(FieldNode field) {
                if (field.getExamples().isEmpty())
                    return field.getDescription();
                StringBuilder text = new StringBuilder(field.getDescription().orElse(""));
                for (var example : field.getExamples()) {
                    text.append('\n').append(EXAMPLE_HEADER).append('\n')
                            .append(exampleYaml.dumpToString(example));
                }
                StringBuilder reference = new StringBuilder();
                appendFieldReference(reference, field.getRef().orElse(field), "");
                if (!reference.isEmpty())
                    text.append(FIELD_REFERENCE_HEADER).append('\n').append(reference);
                return Optional.of(text.toString());
            }

        }
    }
}
