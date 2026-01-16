package com.networknt.config.schema.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.config.schema.FieldNode;
import com.networknt.config.schema.FieldType;
import org.junit.Assert;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.List;

import static com.networknt.config.schema.generator.YamlGenerator.YAML_OPTIONS;

public class YamlGeneratorTest {

    @Test
    public void testYamlPropertyBoolean() {
        final var generator = new YamlGenerator("configTest", "configTest");
        var testDefaultValue = "false";

        var testFieldNode = new FieldNode.Builder(FieldType.BOOLEAN, "root")
                .externalized(true)
                .externalizedKeyName("alternateExternalName")
                .defaultValue(testDefaultValue)
                .build();

        var parsed = generator.convertBooleanNode(testFieldNode).toString();
        Assert.assertTrue(parsed.contains("${configTest.alternateExternalName:false}"));
    }

    @Test
    public void testYamlPropertyNumber() {
        final var generator = new YamlGenerator("configTest", "configTest");
        var defaultValue = "3.38";

        var testFieldNode = new FieldNode.Builder(FieldType.NUMBER, "root")
                .externalizedKeyName("root")
                .externalized(true)
                .defaultValue(defaultValue)
                .build();

        var parsed = generator.convertNumberNode(testFieldNode).toString();
        Assert.assertTrue(parsed.contains("${configTest.root:3.38}"));
    }

    @Test
    public void testYamlPropertyInteger() {
        final var generator = new YamlGenerator("configTest", "configTest");
        var defaultValue = "4";

        var testFieldNode = new FieldNode.Builder(FieldType.INTEGER, "root")
                .externalized(true)
                .externalizedKeyName("root")
                .defaultValue(defaultValue)
                .build();

        var parsed = generator.convertIntegerNode(testFieldNode).toString();
        Assert.assertTrue(parsed.contains("${configTest.root:4}"));
    }

    @Test
    public void testYamlPropertyArray() {
        final var generator = new YamlGenerator("configTest", "configTest");
        var description = "This is a test description.";
        var testDefaultValue = "[\"testValue1\", \"testValue2\"]";
        var testFieldNode = new FieldNode.Builder(FieldType.ARRAY, "arrayNode")
                .externalizedKeyName("arrayNode")
                .externalized(true)
                .description(description)
                .defaultValue(testDefaultValue)
                .build();

        var json = generator.convertArrayNode(testFieldNode).toString();
        Assert.assertTrue(json.contains("${configTest.arrayNode:[\"testValue1\", \"testValue2\"]}"));
    }

    @Test
    public void testYamlPropertyMap() {
        final var generator = new YamlGenerator("configTest", "configTest");

        var multiLineDesc = "This is the multiline description of an inner array.\n" +
                "I can have multiple lines.\n" +
                "Each line starts with a '#'.";
        var testDefaultValue = "[\"testValue1\", \"testValue2\"]";
        var testNestedArrayNode = new FieldNode.Builder(FieldType.ARRAY, "innerArray")
                .description(multiLineDesc)
                .externalizedKeyName("innerArray")
                .externalized(true)
                .defaultValue(testDefaultValue)
                .build();

        var rootDesc = "This is a test description.";
        var testFieldNode = new FieldNode.Builder(FieldType.MAP, "root")
                .description(rootDesc)
                .subObjectDefault(true)
                .childNodes(List.of(testNestedArrayNode))
                .build();

        var json = generator.convertMapNode(testFieldNode).toString();
        Assert.assertTrue(json.contains("${configTest.innerArray:[\"testValue1\", \"testValue2\"]}"));
    }

    @Test
    public void testYamlPropertyObject() {
        final var generator = new YamlGenerator("configTest", "configTest");
        var description = "This is a test description.";
        var node1 = new FieldNode.Builder(FieldType.STRING, "node1")
                .externalized(true)
                .externalizedKeyName("node1")
                .defaultValue("Node1DefaultValue")
                .build();
        var node2 = new FieldNode.Builder(FieldType.STRING, "node2")
                .externalized(true)
                .externalizedKeyName("node2")
                .defaultValue("Node2DefaultValue")
                .build();
        var node3 = new FieldNode.Builder(FieldType.STRING, "node3")
                .externalized(true)
                .externalizedKeyName("node3")
                .defaultValue("Node3DefaultValue")
                .build();

        var propertiesList = List.of(node1, node2, node3);
        var testFieldNode = new FieldNode.Builder(FieldType.OBJECT, "root")
                .description(description)
                .subObjectDefault(true)
                .childNodes(propertiesList)
                .build();

        var parsed = generator.convertObjectNode(testFieldNode).toString();
        Assert.assertTrue(parsed.contains("${configTest.node1:Node1DefaultValue}"));
        Assert.assertTrue(parsed.contains("${configTest.node2:Node2DefaultValue}"));
        Assert.assertTrue(parsed.contains("${configTest.node3:Node3DefaultValue}"));
    }

    @Test
    public void testYamlPropertyFullConfig() {
        final var generator = new YamlGenerator("configTest", "configTest");
        var innerStrNode = new FieldNode.Builder(FieldType.STRING, "innerStringNode")
                .defaultValue("123")
                .externalized(true)
                .externalizedKeyName("innerStringNode")
                .description("This is the inner most string field.")
                .build();

        var otherStringNode = new FieldNode.Builder(FieldType.STRING, "otherNode")
                .defaultValue("abc")
                .externalized(true)
                .externalizedKeyName("otherNode")
                .build();

        var valueNode = new FieldNode.Builder(FieldType.OBJECT, "valueNode")
                .description("This is value type for the map")
                .subObjectDefault(true)
                .childNodes(List.of(innerStrNode, otherStringNode))
                .build();

        var mapNode = new FieldNode.Builder(FieldType.MAP, "map")
                .description("This is the map")
                .subObjectDefault(true)
                .ref(valueNode)
                .build();

        var outerBooleanNode =  new FieldNode.Builder(FieldType.BOOLEAN, "outerBoolean")
                .description("This is the boolean adjacent to the map")
                .defaultValue("false")
                .build();

        var rootNode = new FieldNode.Builder(FieldType.OBJECT, "root")
                .description("This is the root object (mock config)")
                .childNodes(List.of(mapNode, outerBooleanNode))
                .build();

        var result = generator.convertConfigRoot(rootNode).toString();
        Assert.assertTrue(result.contains("${configTest.innerStringNode:123}"));
        Assert.assertTrue(result.contains("${configTest.otherNode:abc}"));
    }


    // TODO[BUG] - This unit test hits the bug!
    @Test
    public void complexConfigStructureYamlGenerationTest() throws JsonProcessingException {
        final var generator = new YamlGenerator("configTest", "configTest");
        /*
         * ...
         * # int1 description
         * integer1:${configTest.integer1:}
         * ...
         * */
        var int1 = FieldType.INTEGER.newBuilder("integer1").description("int1 description").externalized(true).externalizedKeyName("integer1").build();
        /*
        * ...
        * string1:${configTest.strstr1:abc123}
        * ...
        * */
        var str1 = FieldType.STRING.newBuilder("string1").externalized(true).externalizedKeyName("strstr1").defaultValue("abc123").build();

        /*
        * ...
        * obj1:
        *   # int1 description
        *   integer1: ${configTest.integer1:}
        *   string1:${configTest.strstr1:abc123}
        * ...
        * */
        var obj1 = FieldType.OBJECT.newBuilder("obj1")
                .subObjectDefault(true)
                .childNodes(List.of(int1, str1))
                .build();

        /*
         * ...
         * # int2 description
         * integer2:${configTest.integer2:3}
         * ...
         * */
        var int2 = FieldType.INTEGER.newBuilder("integer2").description("int2 description").externalized(true).externalizedKeyName("integer1").defaultValue("3").build();
        /*
         * ...
         * string2:${configTest.strstr2:}
         * ...
         * */
        var str2 = FieldType.STRING.newBuilder("string2").externalized(true).externalizedKeyName("strstr2").build();

        var val1 = FieldType.STRING.newBuilder("val1").pattern("somePattern").maxLength(100).build();
        var arr1 = FieldType.ARRAY.newBuilder("arr1").defaultValue("[\"test1\", \"test2\"]").ref(val1).build();

        var ref1 = FieldType.OBJECT.newBuilder("ref1").childNodes(List.of(int2, str2, arr1, obj1)).build();
        /*
         * ...
         * obj2:
         *   # int2 description
         *   integer2:${configTest.integer2:3}
         *   string2:${configTest.strstr2:}
         * ...
         * */
        var obj2 = FieldType.OBJECT.newBuilder("obj1")
                .subObjectDefault(true)
                .description("This is a multiline description.\n This is the second line.")
                .ref(ref1)
                .build();

        var obj3 = FieldType.OBJECT.newBuilder("obj3")
                .description("This is a multiline description.\n This is the second line.")
                .subObjectDefault(true)
                .childNodes(List.of(obj2))
                .build();

        var root = FieldType.OBJECT.newBuilder("root").childNodes(List.of(obj3)).build();
        var result = generator.convertConfigRoot(root);
        ObjectMapper mapper = new ObjectMapper();
        var printRes = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        System.out.println(printRes);
        final var yaml = new Yaml(new YamlGenerator.YamlCommentRepresenter(YAML_OPTIONS, root), YAML_OPTIONS);
        final var fileContent = yaml.dump(result);
        System.out.println(fileContent);
    }



}
