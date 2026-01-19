package com.networknt.config.schema.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.config.schema.FieldNode;
import com.networknt.config.schema.FieldType;
import com.networknt.config.schema.Format;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;

public class JsonSchemaGeneratorTest {

    @Test
    public void testJsonSchemaString() {
        final var generator = new JsonSchemaGenerator("configTest", "configTest");

        var testDescription = "This is a test description.";
        var testDefaultValue = "abc123";
        var testMinLength = 10;
        var testMaxLength = 20;
        var testPattern = "112233";
        var testFormat = Format.date;

        var testFieldNode = new FieldNode.Builder(FieldType.STRING, "root")
                .description(testDescription)
                .defaultValue(testDefaultValue)
                .minLength(testMinLength)
                .maxLength(testMaxLength)
                .pattern(testPattern)
                .format(testFormat)
                .build();

        var parsed = generator.convertStringNode(testFieldNode);
        Assert.assertEquals(FieldType.STRING.toString(), parsed.get("type"));
        Assert.assertEquals(testDescription, parsed.get("description"));
        Assert.assertEquals(testDefaultValue, parsed.get("default"));
        Assert.assertEquals(testMinLength, parsed.get("minLength"));
        Assert.assertEquals(testMaxLength, parsed.get("maxLength"));
        Assert.assertEquals(testPattern, parsed.get("pattern"));
        Assert.assertEquals(testFormat, parsed.get("format"));
    }

    @Test
    public void testJsonSchemaBoolean() {
        final var generator = new JsonSchemaGenerator("configTest", "configTest");
        var testDescription = "This is a test description.";
        var testDefaultValue = "false";

        var testFieldNode = new FieldNode.Builder(FieldType.BOOLEAN, "root")
                .description(testDescription)
                .defaultValue(testDefaultValue)
                .build();

        var parsed = generator.convertBooleanNode(testFieldNode);
        Assert.assertEquals(FieldType.BOOLEAN.toString(), parsed.get("type"));
        Assert.assertEquals(testDescription, parsed.get("description"));
        Assert.assertEquals(false, parsed.get("default")); // "false" should get parsed as the real boolean value.
    }

    @Test
    public void testJsonSchemaNumber() {
        final var generator = new JsonSchemaGenerator("configTest", "configTest");
        var description = "This is a test description.";
        var defaultValue = "3.38";
        var min = 1.0;
        var max = 8.555;
        var exclMax = true;
        var exclMin = true;
        var format = Format.float64;
        var multiple = 0.2;

        var testFieldNode = new FieldNode.Builder(FieldType.NUMBER, "root")
                .description(description)
                .defaultValue(defaultValue)
                .min(min)
                .max(max)
                .exclusiveMin(exclMin)
                .exclusiveMax(exclMax)
                .format(format)
                .multipleOf(multiple)
                .build();

        var parsed = generator.convertNumberNode(testFieldNode);
        Assert.assertEquals(FieldType.NUMBER.toString(), parsed.get("type"));
        Assert.assertEquals(description, parsed.get("description"));
        Assert.assertEquals(3.38, parsed.get("default"));
        Assert.assertEquals(min, parsed.get("minimum"));
        Assert.assertEquals(max, parsed.get("maximum"));
        Assert.assertEquals(exclMin, parsed.get("exclusiveMin"));
        Assert.assertEquals(exclMax, parsed.get("exclusiveMax"));
        Assert.assertEquals(format, parsed.get("format"));
        Assert.assertEquals(multiple, parsed.get("multipleOf"));
    }

    @Test
    public void testJsonSchemaInteger() {
        final var generator = new JsonSchemaGenerator("configTest", "configTest");
        var description = "This is a test description.";
        var defaultValue = "4";
        var min = 2;
        var max = 8;
        var exclMax = true;
        var exclMin = false;
        var format = Format.int64;
        var multiple = 2;

        var testFieldNode = new FieldNode.Builder(FieldType.INTEGER, "root")
                .description(description)
                .defaultValue(defaultValue)
                .min(min)
                .max(max)
                .exclusiveMin(exclMin)
                .exclusiveMax(exclMax)
                .format(format)
                .multipleOf(multiple)
                .build();

        var parsed = generator.convertIntegerNode(testFieldNode);
        Assert.assertEquals(FieldType.INTEGER.toString(), parsed.get("type"));
        Assert.assertEquals(description, parsed.get("description"));
        Assert.assertEquals(4, parsed.get("default"));
        Assert.assertEquals(min, parsed.get("minimum"));
        Assert.assertEquals(max, parsed.get("maximum"));

        // Since false is the default, we should expect that exclusiveMin is null.
        Assert.assertNull(parsed.get("exclusiveMin"));

        Assert.assertEquals(exclMax, parsed.get("exclusiveMax"));
        Assert.assertEquals(format, parsed.get("format"));
        Assert.assertEquals(multiple, parsed.get("multipleOf"));
    }

    @Test
    public void testJsonSchemaArray() {
        final var generator = new JsonSchemaGenerator("configTest", "configTest");
        var description = "This is a test description.";
        var min = 2;
        var max = 8;
        var unique = true;
        var node1 = new FieldNode.Builder(FieldType.STRING, "node1").build();
        var testDefaultValue = "[\"testValue1\", \"testValue2\"]";
        var testFieldNode = new FieldNode.Builder(FieldType.ARRAY, "root")
                .description(description)
                .minItems(min)
                .maxItems(max)
                .defaultValue(testDefaultValue)
                .uniqueItems(unique)
                .childNodes(List.of(node1))
                .build();

        var parsed = generator.convertArrayNode(testFieldNode);
        Assert.assertEquals(FieldType.ARRAY.toString(), parsed.get("type"));
        Assert.assertEquals(description, parsed.get("description"));
        Assert.assertEquals(min, parsed.get("minItems"));
        Assert.assertEquals(max, parsed.get("maxItems"));
        Assert.assertEquals(unique, parsed.get("uniqueItems"));
        Assert.assertEquals("testValue1", ((List<String>)parsed.get("default")).get(0));
        Assert.assertEquals(FieldType.STRING.toString(), ((LinkedHashMap<String, Object>)parsed.get("items")).get("type"));
    }

    @Test
    public void testJsonSchemaMap() {
        final var generator = new JsonSchemaGenerator("configTest", "configTest");
        var description = "This is a test description.";
        var min = 2;
        var max = 8;
        var unique = true;
        var node1 = new FieldNode.Builder(FieldType.STRING, "node1").build();
        var testDefaultValue = "[\"testValue1\", \"testValue2\"]";
        var testNestedArrayNode = new FieldNode.Builder(FieldType.ARRAY, "innerArray")
                .description(description)
                .minItems(min)
                .maxItems(max)
                .defaultValue(testDefaultValue)
                .uniqueItems(unique)
                .childNodes(List.of(node1))
                .build();

        var testFieldNode = new FieldNode.Builder(FieldType.MAP, "root")
                .description(description)
                .childNodes(List.of(testNestedArrayNode))
                .build();

        var parsed = generator.convertMapNode(testFieldNode);
        Assert.assertEquals(FieldType.MAP.toString(), parsed.get("type"));
        Assert.assertEquals(description, parsed.get("description"));
        Assert.assertEquals(FieldType.ARRAY.toString(), ((LinkedHashMap<String, Object>)parsed.get("additionalProperties")).get("type"));
    }

    @Test
    public void testJsonSchemaObject() {
        final var generator = new JsonSchemaGenerator("configTest", "configTest");
        var description = "This is a test description.";
        var node1 = new FieldNode.Builder(FieldType.STRING, "node1").build();
        var node2 = new FieldNode.Builder(FieldType.STRING, "node2").build();
        var node3 = new FieldNode.Builder(FieldType.STRING, "node3").build();

        var propertiesList = List.of(node1, node2, node3);
        var testFieldNode = new FieldNode.Builder(FieldType.OBJECT, "root")
                .description(description)
                .childNodes(propertiesList)
                .build();

        var parsed = generator.convertObjectNode(testFieldNode);
        System.out.println(parsed);
        Assert.assertEquals(FieldType.OBJECT.toString(), parsed.get("type"));
        Assert.assertEquals(description, parsed.get("description"));

        Assert.assertEquals(propertiesList.size(), ((LinkedHashMap<String, Object>)parsed.get("properties")).size());
    }

    @Test
    public void testJsonSchemaRootOutput() {
        final var generator = new JsonSchemaGenerator("configTest", "configTest");
        var pattern = "^[a-zA-Z0-9-_]*$";
        var innerStrNode = new FieldNode.Builder(FieldType.STRING, "innerStringNode")
                .pattern(pattern)
                .build();

        var otherStringNode = new FieldNode.Builder(FieldType.STRING, "otherNode").build();

        var valueNode = new FieldNode.Builder(FieldType.OBJECT, "valueNode")
                .childNodes(List.of(innerStrNode, otherStringNode))
                .build();

        var mapNode = new FieldNode.Builder(FieldType.MAP, "map")
                .ref(valueNode)
                .build();

        var outerBooleanNode =  new FieldNode.Builder(FieldType.BOOLEAN, "outerBoolean")
                .defaultValue("false")
                .build();

        var rootNode = new FieldNode.Builder(FieldType.OBJECT, "root")
                .childNodes(List.of(mapNode, outerBooleanNode))
                .build();

        var result = generator.addJsonSchemaRootInfo(rootNode);

        var requiredArray = result.get(JsonSchemaGenerator.REQUIRED_KEY);
        Assert.assertEquals(2, ((List<String>) requiredArray).size());

        var rootProps = (LinkedHashMap<String, Object>) result.get(JsonSchemaGenerator.PROPERTIES_KEY);
        Assert.assertNotNull(rootProps);

        var mapProps = (LinkedHashMap<String, Object>) rootProps.get("map");
        Assert.assertNotNull(mapProps);

        var innerString = ((LinkedHashMap<String, Object>)((LinkedHashMap<String, Object>) mapProps.get(JsonSchemaGenerator.ADDITIONAL_PROPERTIES_KEY)).get(JsonSchemaGenerator.PROPERTIES_KEY)).get("innerStringNode");
        Assert.assertNotNull(innerString);

        Assert.assertEquals(pattern, ((LinkedHashMap<String, Object>)innerString).get(JsonSchemaGenerator.PATTERN_KEY));

        var innerBooleanDefault = ((LinkedHashMap<String, Object>)rootProps.get("outerBoolean")).get(JsonSchemaGenerator.DEFAULT_KEY);
        Assert.assertEquals(false, innerBooleanDefault);
    }

    @Test
    public void testMultiClassJsonSchema() throws JsonProcessingException {
        final var testOutput = "{\n" +
                "  \"type\" : \"object\",\n" +
                "  \"properties\" : {\n" +
                "    \"multiClassMap\" : {\n" +
                "      \"type\" : \"object\",\n" +
                "      \"additionalProperties\" : {\n" +
                "        \"oneOf\" : [ {\n" +
                "          \"type\" : \"string\"\n" +
                "        }, {\n" +
                "          \"type\" : \"object\",\n" +
                "          \"additionalProperties\" : {\n" +
                "            \"type\" : \"string\"\n" +
                "          }\n" +
                "        } ]\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        final var generator = new JsonSchemaGenerator("configTest", "configTest");
        var type1 = FieldType.STRING.newBuilder("type1").build();
        var type2 = FieldType.MAP.newBuilder("type2")
                .ref(FieldType.STRING.newBuilder("mapType1")
                        .build()).build();

        var multiClassMap = FieldType.MAP.newBuilder("multiClassMap").externalizedKeyName("multi").oneOf(List.of(type1, type2)).build();
        var steppingClass = FieldType.OBJECT.newBuilder("steppingClass").childNodes(List.of(multiClassMap)).build();
        var rootObj = FieldType.OBJECT.newBuilder("root").ref(steppingClass).build();
        var parsed = generator.convertConfigRoot(rootObj);
        ObjectMapper objectMapper = new ObjectMapper();
        var result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsed);
        Assert.assertEquals(
                testOutput.replace("\n", "").replace("\r", "").replace(" ", ""),
                result.replace("\n", "").replace("\r", "").replace(" ", "")
        );
    }
}
