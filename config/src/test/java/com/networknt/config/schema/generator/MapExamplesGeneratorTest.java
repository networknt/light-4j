package com.networknt.config.schema.generator;

import com.networknt.config.schema.FieldNode;
import com.networknt.config.schema.FieldType;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MapExamplesGeneratorTest {
    @Test
    void exampleIsAFieldValueInJsonAndCopyableYamlComments() throws Exception {
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
        assertThrows(IllegalArgumentException.class, () ->
                FieldType.MAP.newBuilder("profiles").examples(new String[]{"[]"}).build());
        var array = FieldType.ARRAY.newBuilder("values").itemsPattern("\\S")
                .ref(FieldType.INTEGER.newBuilder("value").build()).build();
        assertThrows(IllegalArgumentException.class, () -> generator.convertArrayNode(array));
    }
}
