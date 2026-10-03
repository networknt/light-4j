package com.networknt.config.schema.generator;

import com.networknt.config.schema.FieldType;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MapExamplesGeneratorTest {
    @Test
    void exampleIsAFieldValueInJsonAndCopyableYamlComments() {
        var map = FieldType.MAP.newBuilder("profiles")
                .externalizedKeyName("profiles")
                .description("Short help.")
                .examples(new String[]{"{\"github\":{\"signatureHeader\":\"X-Signature\"}}"})
                .requiredProperties(new String[]{"signatureHeader"})
                .ref(FieldType.OBJECT.newBuilder("profile").childNodes(List.of(
                        FieldType.STRING.newBuilder("signatureHeader").build())).build())
                .build();
        var json = new JsonSchemaGenerator("hmac", "hmac").convertMapNode(map);
        assertEquals("Short help.", json.get("description"));
        assertEquals(List.of(Map.of("github", Map.of("signatureHeader", "X-Signature"))), json.get("examples"));
        assertEquals(List.of("signatureHeader"), ((Map<?, ?>) json.get("additionalProperties")).get("required"));
        var root = FieldType.OBJECT.newBuilder("root").childNodes(List.of(map)).build();
        StringWriter writer = new StringWriter();
        new YamlGenerator("hmac", "hmac").writeSchemaToFile(writer, root);
        String yaml = writer.toString();
        assertTrue(yaml.contains("# github:\n#   signatureHeader: X-Signature"));
        Map<?, ?> active = new Yaml().load(yaml);
        assertEquals("${hmac.profiles:}", active.get("profiles"));
    }

    @Test
    void fieldReferenceIncludesObjectHelpAndOmitsEmptyHeaders() {
        var secrets = FieldType.OBJECT.newBuilder("secrets").description("Required secret sources.")
                .ref(FieldType.OBJECT.newBuilder("secretConfig").childNodes(List.of(
                        FieldType.STRING.newBuilder("name").description("Environment name.").build())).build()).build();
        var values = FieldType.OBJECT.newBuilder("profile").childNodes(List.of(secrets)).build();
        String yaml = renderExample(values);
        assertTrue(yaml.contains("secrets: Required secret sources."));
        assertTrue(yaml.contains("secrets.name: Environment name."));
        for (var value : List.of(FieldType.STRING.newBuilder("scalar").build(),
                FieldType.ARRAY.newBuilder("array").ref(FieldType.STRING.newBuilder("item").build()).build(),
                FieldType.OBJECT.newBuilder("union").oneOf(List.of(values)).build())) {
            assertFalse(renderExample(value).contains(YamlGenerator.FIELD_REFERENCE_HEADER));
        }
    }

    private String renderExample(com.networknt.config.schema.FieldNode value) {
        var map = FieldType.MAP.newBuilder("values").externalizedKeyName("values")
                .examples(new String[]{"{\"example\": {}}"}).ref(value).build();
        var root = FieldType.OBJECT.newBuilder("root").childNodes(List.of(map)).build();
        var writer = new StringWriter();
        new YamlGenerator("test", "test").writeSchemaToFile(writer, root);
        return writer.toString();
    }

    @Test
    void stringUnionsAllowPatternsAndMixedUnionsRejectThem() {
        var generator = new JsonSchemaGenerator("test", "test");
        var strings = List.of(FieldType.STRING.newBuilder("first").build(),
                FieldType.STRING.newBuilder("second").build());
        var mixed = List.of(strings.get(0), FieldType.INTEGER.newBuilder("number").build());
        for (String keyword : List.of("oneOf", "allOf", "anyOf")) {
            var builder = FieldType.ARRAY.newBuilder("values").itemsPattern("\\S");
            var mixedBuilder = FieldType.ARRAY.newBuilder("values").itemsPattern("\\S");
            switch (keyword) {
                case "oneOf" -> { builder.oneOf(strings); mixedBuilder.oneOf(mixed); }
                case "allOf" -> { builder.allOf(strings); mixedBuilder.allOf(mixed); }
                default -> { builder.anyOf(strings); mixedBuilder.anyOf(mixed); }
            }
            var items = (Map<?, ?>) generator.convertArrayNode(builder.build()).get("items");
            assertEquals("\\S", items.get("pattern"));
            assertEquals(2, ((List<?>) items.get(keyword)).size());
            var invalid = mixedBuilder.build();
            assertThrows(IllegalArgumentException.class, () -> generator.convertArrayNode(invalid));
        }
    }

    @Test
    void debugSerializationIncludesNewMetadata() {
        var node = FieldType.MAP.newBuilder("profiles")
                .examples(new String[]{"{\"github\": {}}"})
                .requiredProperties(new String[]{"signatureHeader"})
                .itemsPattern("\\S").build();
        var json = new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(node);
        assertTrue(json.path("examples").get(0).has("github"));
        assertEquals("signatureHeader", json.path("requiredProperties").get(0).asText());
        assertEquals("\\S", json.path("itemsPattern").asText());
    }

    @Test
    void missingMetadataDoesNotAddExamplesOrRequiredProperties() {
        var map = FieldType.MAP.newBuilder("profiles")
                .ref(FieldType.STRING.newBuilder("value").build()).build();
        assertEquals(Map.of("type", "object", "additionalProperties", Map.of("type", "string")),
                new JsonSchemaGenerator("hmac", "hmac").convertMapNode(map));
    }

    @Test
    void stringItemPatternIsPreservedForArrayValuedMap() {
        var array = FieldType.ARRAY.newBuilder("values").minItems(1).maxItems(2)
                .uniqueItems(true).itemsPattern("\\S")
                .ref(FieldType.STRING.newBuilder("value").build()).build();
        var map = FieldType.MAP.newBuilder("selectors").ref(array).build();
        var value = (Map<?, ?>) new JsonSchemaGenerator("hmac", "hmac")
                .convertMapNode(map).get("additionalProperties");
        assertEquals("array", value.get("type"));
        assertEquals(1, value.get("minItems"));
        assertEquals(2, value.get("maxItems"));
        assertEquals(true, value.get("uniqueItems"));
        assertEquals(Map.of("type", "string", "pattern", "\\S"), value.get("items"));
    }

    @Test
    void malformedExamplesAndNonStringItemPatternsFailGeneration() {
        var generator = new JsonSchemaGenerator("hmac", "hmac");
        var builder = FieldType.MAP.newBuilder("profiles");
        var invalidExamples = new String[]{"[]"};
        assertThrows(IllegalArgumentException.class, () -> builder.examples(invalidExamples));
        var array = FieldType.ARRAY.newBuilder("values").itemsPattern("\\S")
                .ref(FieldType.INTEGER.newBuilder("value").build()).build();
        assertThrows(IllegalArgumentException.class, () -> generator.convertArrayNode(array));
    }
}
