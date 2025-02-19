package com.networknt.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.config.schema.generator.JsonSchemaGenerator;
import com.networknt.config.schema.generator.YamlGenerator;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

import static org.junit.Assert.*;

public class JsonSchemaGeneratorTest {

    private static final String testMetadata2 = "{\n" +
            "  \"properties\" : {\n" +
            "    \"headers\" : {\n" +
            "      \"type\" : \"array\",\n" +
            "      \"configFieldName\" : \"headers\",\n" +
            "      \"description\" : \"Output header elements. You can add more if you want. If multiple values, you can use a comma separated\\nstring as default value in the template and values.yml. You can also use a list of strings in YAML format.\",\n" +
            "      \"externalized\" : true,\n" +
            "      \"items\" : {\n" +
            "        \"type\" : \"string\"\n" +
            "      },\n" +
            "      \"minItems\" : 0,\n" +
            "      \"maxItems\" : 2147483647,\n" +
            "      \"uniqueItems\" : false,\n" +
            "      \"contains\" : false,\n" +
            "      \"useSubObjectDefault\" : false,\n" +
            "      \"defaultValueJsonString\" : \"[\\\"X-Correlation-Id\\\", \\\"X-Traceability-Id\\\",\\\"caller_id\\\"]\"\n" +
            "    },\n" +
            "    \"audit\" : {\n" +
            "      \"type\" : \"array\",\n" +
            "      \"configFieldName\" : \"audit\",\n" +
            "      \"description\" : \"Output audit elements. You can add more if you want. If multiple values, you can use a comma separated\\nstring as default value in the template and values.yml. You can also use a list of strings in YAML format.\",\n" +
            "      \"externalized\" : true,\n" +
            "      \"items\" : {\n" +
            "        \"type\" : \"string\"\n" +
            "      },\n" +
            "      \"minItems\" : 0,\n" +
            "      \"maxItems\" : 2147483647,\n" +
            "      \"uniqueItems\" : false,\n" +
            "      \"contains\" : false,\n" +
            "      \"useSubObjectDefault\" : false,\n" +
            "      \"defaultValueJsonString\" : \"[\\\"client_id\\\", \\\"user_id\\\", \\\"scope_client_id\\\", \\\"endpoint\\\", \\\"serviceId\\\"]\"\n" +
            "    },\n" +
            "    \"statusCode\" : {\n" +
            "      \"type\" : \"boolean\",\n" +
            "      \"configFieldName\" : \"statusCode\",\n" +
            "      \"description\" : \"Output response status code.\",\n" +
            "      \"externalized\" : true,\n" +
            "      \"defaultValue\" : true\n" +
            "    },\n" +
            "    \"responseTime\" : {\n" +
            "      \"type\" : \"boolean\",\n" +
            "      \"configFieldName\" : \"responseTime\",\n" +
            "      \"description\" : \"Output response time.\",\n" +
            "      \"externalized\" : true,\n" +
            "      \"defaultValue\" : true\n" +
            "    },\n" +
            "    \"auditOnError\" : {\n" +
            "      \"type\" : \"boolean\",\n" +
            "      \"configFieldName\" : \"auditOnError\",\n" +
            "      \"description\" : \"when auditOnError is true:\\n - it will only log when status code >= 400\\nwhen auditOnError is false:\\n - it will log on every request\\nlog level is controlled by logLevel\",\n" +
            "      \"externalized\" : true,\n" +
            "      \"defaultValue\" : false\n" +
            "    },\n" +
            "    \"mask\" : {\n" +
            "      \"type\" : \"boolean\",\n" +
            "      \"configFieldName\" : \"mask\",\n" +
            "      \"description\" : \"Enable mask in the audit log\",\n" +
            "      \"externalized\" : true,\n" +
            "      \"defaultValue\" : true\n" +
            "    },\n" +
            "    \"timestampFormat\" : {\n" +
            "      \"type\" : \"string\",\n" +
            "      \"configFieldName\" : \"timestampFormat\",\n" +
            "      \"description\" : \"the format for outputting the timestamp, if the format is not specified or invalid, will use a long value.\\nfor some users that will process the audit log manually, you can use yyyy-MM-dd'T'HH:mm:ss.SSSZ as format.\",\n" +
            "      \"externalized\" : true,\n" +
            "      \"defaultValue\" : \"\",\n" +
            "      \"minLength\" : 0,\n" +
            "      \"maxLength\" : 2147483647,\n" +
            "      \"pattern\" : \"\",\n" +
            "      \"format\" : \"none\"\n" +
            "    },\n" +
            "    \"requestBodyMaxSize\" : {\n" +
            "      \"type\" : \"integer\",\n" +
            "      \"configFieldName\" : \"requestBodyMaxSize\",\n" +
            "      \"description\" : \"The limit of the request body to put into the audit entry if requestBody is in the list of audit. If the\\nrequest body is bigger than the max size, it will be truncated to the max size. The default value is 4096.\",\n" +
            "      \"externalized\" : true,\n" +
            "      \"defaultValue\" : 4096,\n" +
            "      \"minimum\" : -2147483648,\n" +
            "      \"maximum\" : 2147483647,\n" +
            "      \"exclusiveMin\" : false,\n" +
            "      \"exclusiveMax\" : false,\n" +
            "      \"multipleOf\" : 0,\n" +
            "      \"format\" : \"int32\"\n" +
            "    },\n" +
            "    \"responseBodyMaxSize\" : {\n" +
            "      \"type\" : \"integer\",\n" +
            "      \"configFieldName\" : \"responseBodyMaxSize\",\n" +
            "      \"description\" : \"The limit of the response body to put into the audit entry if responseBody is in the list of audit. If the\\nresponse body is bigger than the max size, it will be truncated to the max size. The default value is 4096.\",\n" +
            "      \"externalized\" : true,\n" +
            "      \"defaultValue\" : 4096,\n" +
            "      \"minimum\" : -2147483648,\n" +
            "      \"maximum\" : 2147483647,\n" +
            "      \"exclusiveMin\" : false,\n" +
            "      \"exclusiveMax\" : false,\n" +
            "      \"multipleOf\" : 0,\n" +
            "      \"format\" : \"int32\"\n" +
            "    },\n" +
            "    \"enabled\" : {\n" +
            "      \"type\" : \"boolean\",\n" +
            "      \"configFieldName\" : \"enabled\",\n" +
            "      \"description\" : \"Enable Audit Logging\",\n" +
            "      \"externalized\" : true,\n" +
            "      \"defaultValue\" : true\n" +
            "    }\n" +
            "  },\n" +
            "  \"type\" : \"object\"\n" +
            "}";
    private static final String testMetadata = "{\n" +
            "  \"type\": \"object\",\n" +
            "  \"properties\": {\n" +
            "    \"pathPrefixAuths\": {\n" +
            "      \"minItems\": 0,\n" +
            "      \"maxItems\": 2147483647,\n" +
            "      \"contains\": false,\n" +
            "      \"externalized\": true,\n" +
            "      \"useSubObjectDefault\": false," +
            "      \"defaultValue\": \"\",\n" +
            "      \"uniqueItems\": false,\n" +
            "      \"configFieldName\": \"pathPrefixAuths\",\n" +
            "      \"description\": \"\",\n" +
            "      \"type\": \"array\",\n" +
            "      \"items\": {\n" +
            "        \"type\": \"object\",\n" +
            "        \"properties\": {\n" +
            "          \"headerName\": {\n" +
            "            \"externalized\": false,\n" +
            "            \"defaultValue\": \"\",\n" +
            "            \"minLength\": 0,\n" +
            "            \"configFieldName\": \"headerName\",\n" +
            "            \"pattern\": \"^[a-zA-Z0-9-_]*$\",\n" +
            "            \"format\": \"none\",\n" +
            "            \"description\": \"\",\n" +
            "            \"type\": \"string\",\n" +
            "            \"maxLength\": 2147483647\n" +
            "          },\n" +
            "          \"apiKey\": {\n" +
            "            \"externalized\": false,\n" +
            "            \"defaultValue\": \"\",\n" +
            "            \"minLength\": 0,\n" +
            "            \"configFieldName\": \"apiKey\",\n" +
            "            \"pattern\": \"\",\n" +
            "            \"format\": \"none\",\n" +
            "            \"description\": \"\",\n" +
            "            \"type\": \"string\",\n" +
            "            \"maxLength\": 2147483647\n" +
            "          },\n" +
            "          \"pathPrefix\": {\n" +
            "            \"externalized\": false,\n" +
            "            \"defaultValue\": \"\",\n" +
            "            \"minLength\": 0,\n" +
            "            \"configFieldName\": \"pathPrefix\",\n" +
            "            \"pattern\": \"^/.*\",\n" +
            "            \"format\": \"none\",\n" +
            "            \"description\": \"\",\n" +
            "            \"type\": \"string\",\n" +
            "            \"maxLength\": 2147483647\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    },\n" +
            "    \"enabled\": {\n" +
            "      \"externalized\": true,\n" +
            "      \"defaultValue\": false,\n" +
            "      \"configFieldName\": \"enabled\",\n" +
            "      \"description\": \"Enable or disable the api key filter.\",\n" +
            "      \"type\": \"boolean\"\n" +
            "    },\n" +
            "    \"hashEnabled\": {\n" +
            "      \"externalized\": true,\n" +
            "      \"defaultValue\": false,\n" +
            "      \"configFieldName\": \"hashEnabled\",\n" +
            "      \"description\": \"If API key hash is enabled. The API key will be hashed with PBKDF2WithHmacSHA1 before it is\\nstored in the config file. It is more secure than put the encrypted key into the config file.\\nThe default value is false. If you want to enable it, you need to use the following repo\\nhttps://github.com/networknt/light-hash command line tool to hash the clear text key.\",\n" +
            "      \"type\": \"boolean\"\n" +
            "    }\n" +
            "  }\n" +
            "}";
    private static final ObjectMapper MAPPER = new ObjectMapper();


    @Test
    public void writeJsonSchemaToFile() throws IOException {
        final var metadata = MAPPER.readValue(testMetadata, new TypeReference<LinkedHashMap<String, Object>>() {});
        final var generator = new JsonSchemaGenerator("apikey-test");
        generator.writeSchemaToFile(new FileWriter("./src/test/resources/config/apikey-test.json"),  metadata);
        final var file1 = new File("src/test/resources/config/apikey-test.json");
        final var file2 = new File("src/test/resources/config/apikey-compare.json");
        assertEquals(Files.readString(file1.toPath(), StandardCharsets.UTF_8).replaceAll("\\s", ""), Files.readString(file2.toPath(), StandardCharsets.UTF_8).replaceAll("\\s", ""));
    }

    @Test
    public void writeYamlSchemaToFile() throws IOException {
        final var metadata = MAPPER.readValue(testMetadata2, new TypeReference<LinkedHashMap<String, Object>>() {});
        final var generator = new YamlGenerator("audit-test");
        generator.writeSchemaToFile(new FileWriter("./src/test/resources/config/audit-test.yaml"),  metadata);
        final var file1 = new File("src/test/resources/config/audit-test.yaml");
        final var file2 = new File("src/test/resources/config/audit-compare.yaml");
        assertTrue(Arrays.equals(Files.readAllBytes(file1.toPath()), Files.readAllBytes(file2.toPath())));
    }

}
