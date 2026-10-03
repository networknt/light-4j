package com.networknt.config.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.config.schema.generator.JsonSchemaGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class MetadataParserTest {
    @TempDir
    Path output;

    @Test
    void nestedAndTopLevelArrayUnionsRetainAllItemTypes() throws IOException {
        String source = """
                package fixture;
                import com.networknt.config.schema.*;
                class ArrayUnions {
                    @MapField(configFieldName = "one", valueArray = @ArrayField(
                        configFieldName = "values", itemsOneOf = {String.class, Integer.class}))
                    Object one;
                    @MapField(configFieldName = "all", valueArray = @ArrayField(
                        configFieldName = "values", itemsAllOf = {String.class, Integer.class}))
                    Object all;
                    @MapField(configFieldName = "any", valueArray = @ArrayField(
                        configFieldName = "values", itemsAnyOf = {String.class, Integer.class}))
                    Object any;
                    @MapField(configFieldName = "plain", valueArray = @ArrayField(
                        configFieldName = "values", items = String.class, minItems = 1, maxItems = 2))
                    Object plain;
                    @ArrayField(configFieldName = "top", itemsOneOf = {String.class, Integer.class})
                    Object top;
                }
                """;
        var processor = compile(source);
        var properties = processor.schema.path("properties");
        for (String name : List.of("one", "all", "any")) {
            var value = properties.path(name).path("additionalProperties");
            assertEquals("array", value.path("type").asText());
            var items = value.path("items").path(name + "Of");
            assertEquals(2, items.size(), name + " must retain nested item union");
            assertEquals("string", items.get(0).path("type").asText());
            assertEquals("integer", items.get(1).path("type").asText());
        }
        assertEquals(properties.path("one").path("additionalProperties").path("items"),
                properties.path("top").path("items"));
        var plain = properties.path("plain").path("additionalProperties");
        assertEquals("string", plain.path("items").path("type").asText());
        assertEquals(1, plain.path("minItems").asInt());
        assertEquals(2, plain.path("maxItems").asInt());
    }

    @Test
    void mapAndObjectUnionsUseTheSameResolutionAndPrimitiveOnlyUnionsFallBack() throws IOException {
        var processor = compile("""
                package fixture;
                import com.networknt.config.schema.*;
                class ArrayUnions {
                    @MapField(configFieldName="map", valueTypeOneOf={String.class, Integer.class}) Object map;
                    @ObjectField(configFieldName="object", refAnyOf={String.class, Integer.class}) Object object;
                    @ArrayField(configFieldName="array", itemsOneOf={int.class}, items=String.class) Object array;
                    @MapField(configFieldName="fallbackMap", valueTypeAllOf={int.class}, valueType=String.class) Object fallbackMap;
                    @ObjectField(configFieldName="fallbackObject", refOneOf={int.class}, ref=String.class) Object fallbackObject;
                }
                """);
        var props = processor.schema.path("properties");
        assertEquals(2, props.path("map").path("additionalProperties").path("oneOf").size());
        assertEquals(2, props.path("object").path("anyOf").size());
        assertEquals("string", props.path("array").path("items").path("type").asText());
        assertEquals("string", props.path("fallbackMap").path("additionalProperties").path("type").asText());
        assertEquals("string", props.path("fallbackObject").path("type").asText());
    }

    @Test
    void arrayValuedMapsRejectConflictingTypeDeclarations() throws IOException {
        for (String conflict : List.of("valueTypeOneOf={int.class}", "valueTypeAllOf={String.class}",
                "valueTypeAnyOf={String.class}", "valueType=String.class")) {
            var processor = compile("""
                    package fixture;
                    import com.networknt.config.schema.*;
                    class ArrayUnions {
                        @MapField(configFieldName="conflict", valueArray=@ArrayField(configFieldName="values"), %s)
                        Object conflict;
                    }
                    """.formatted(conflict));
            assertNotNull(processor.failure, conflict);
            assertTrue(processor.failure.getMessage().contains("valueArray cannot be combined"), conflict);
        }
    }

    @Test
    void publicClassArrayLookupSupportsDirectAnnotationsButDoesNotInspectNestedAnnotations() throws IOException {
        var processor = compile("""
                package fixture;
                import com.networknt.config.schema.*;
                class ArrayUnions {
                    @ArrayField(configFieldName="direct", itemsOneOf={String.class, Integer.class}) Object direct;
                    @ArrayField(configFieldName="empty") Object empty;
                    @MapField(configFieldName="nested", valueArray=@ArrayField(
                        configFieldName="values", itemsOneOf={String.class, Integer.class})) Object nested;
                    Object absent;
                }
                """, new CompatibilityProcessor());
        assertEquals(Optional.of(List.of("java.lang.String", "java.lang.Integer")),
                processor.results.get("direct"));
        assertEquals(Optional.of(List.of()), processor.results.get("empty"));
        assertEquals(Optional.empty(), processor.results.get("nested"));
        assertEquals(Optional.empty(), processor.results.get("absent"));
        assertEquals(Optional.empty(), processor.missingMember);
    }

    private CapturingProcessor compile(String source) throws IOException {
        return compile(source, new CapturingProcessor());
    }

    private <T extends CapturingProcessor> T compile(String source, T processor) throws IOException {
        var compiler = ToolProvider.getSystemJavaCompiler();
        assumeTrue(compiler != null, "Annotation-processing regression needs a JDK");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var files = compiler.getStandardFileManager(diagnostics, null, null)) {
            var unit = new SimpleJavaFileObject(URI.create("string:///fixture/ArrayUnions.java"),
                    JavaFileObject.Kind.SOURCE) {
                @Override
                public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                    return source;
                }
            };
            var task = compiler.getTask(null, files, diagnostics,
                    List.of("-proc:only", "-classpath", System.getProperty("java.class.path"),
                            "-d", output.toString()), null, List.of(unit));
            task.setProcessors(List.of(processor));
            assertTrue(task.call(), () -> diagnostics.getDiagnostics().toString());
        }
        return processor;
    }

    private static class CompatibilityProcessor extends CapturingProcessor {
        private final Map<String, Optional<List<String>>> results = new HashMap<>();
        private Optional<List<String>> missingMember;

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            if (!roundEnv.processingOver() && results.isEmpty()) {
                var root = processingEnv.getElementUtils().getTypeElement("fixture.ArrayUnions");
                for (var element : root.getEnclosedElements()) {
                    String name = element.getSimpleName().toString();
                    results.put(name, AnnotationUtils.getClassArrayMirrors(element, ArrayField.class,
                            "itemsOneOf", processingEnv).map(types -> types.stream().map(Object::toString).toList()));
                    if (name.equals("direct"))
                        missingMember = AnnotationUtils.getClassArrayMirrors(element, ArrayField.class,
                                "missingMember", processingEnv).map(types -> types.stream().map(Object::toString).toList());
                }
            }
            return true;
        }
    }

    private static class CapturingProcessor extends AbstractProcessor {
        private JsonNode schema;
        private IllegalArgumentException failure;

        @Override
        public Set<String> getSupportedAnnotationTypes() {
            return Set.of("*");
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latestSupported();
        }

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            if (!roundEnv.processingOver() && schema == null) {
                var root = processingEnv.getElementUtils().getTypeElement("fixture.ArrayUnions");
                final FieldNode metadata;
                try {
                    metadata = MetadataParser.gatherObjectSchemaData(root, processingEnv).build();
                } catch (IllegalArgumentException e) {
                    failure = e;
                    return true;
                }
                var writer = new StringWriter();
                try {
                    new JsonSchemaGenerator("fixture", "fixture").writeSchemaToFile(writer, metadata);
                    schema = new ObjectMapper().readTree(writer.toString());
                } catch (IOException e) {
                    throw new IllegalStateException("Cannot capture generated fixture schema", e);
                }
            }
            return true;
        }
    }
}
