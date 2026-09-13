/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.mask;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MaskTest {

    @BeforeAll
    public static void runOnceBeforeClass() {
        Configuration.setDefaults(new Configuration.Defaults() {

            private final JsonProvider jsonProvider = new JacksonJsonProvider();
            private final MappingProvider mappingProvider = new JacksonMappingProvider();

            @Override
            public JsonProvider jsonProvider() {
                return jsonProvider;
            }

            @Override
            public MappingProvider mappingProvider() {
                return mappingProvider;
            }

            @Override
            public Set<Option> options() {
                return EnumSet.noneOf(Option.class);
            }
        });
    }

    @Test
    public void testMaskString() {
        String url1 = "/v1/customer?sin=123456789&password=secret&number=1234567890123456";
        String output = Mask.maskString(url1, "uri");
        System.out.println("ouput = " + output);
        Assertions.assertEquals("/v1/customer?sin=masked&password=******&number=----------------", output);
    }

    @Test
    public void testMaskQueryParameter() {
        String test = "aaaa";
        String output = Mask.maskRegex(test, "queryParameter", "accountNo");
        System.out.println("output = " + output);
        Assertions.assertEquals(output, "****");
    }

    @Test
    public void testMaskRequestHeader() {
        String testHeader1 = "test";
        String testHeader2 = "tests";
        String output1 = Mask.maskRegex(testHeader1, "requestHeader", "header1");
        System.out.println("output1 = " + output1);
        Assertions.assertEquals(output1, "****");
        String output2 = Mask.maskRegex(testHeader2, "requestHeader", "header2");
        Assertions.assertEquals(output2, "*****");
    }

    @Test
    public void testMaskResponseHeader() {
        String testHeader = "header";
        String output = Mask.maskRegex(testHeader, "responseHeader", "header3");
        System.out.println("output = " + output);
        Assertions.assertEquals(output, "******");
    }
    @Test
    public void testMaskRequestBody() {
        String input = "{\"name\":\"Steve\",\"contact\":{\"phone\":\"416-111-1111\"},\"password\":\"secret\"}";
        String output = Mask.maskJson(input, "test1");
        System.out.println(output);
        Assertions.assertEquals(JsonPath.parse(output).read("$.contact.phone"), "************");
        Assertions.assertEquals(JsonPath.parse(output).read("$.password"), "******");
        Assertions.assertEquals(output, "{\"name\":\"Steve\",\"contact\":{\"phone\":\"************\"},\"password\":\"******\"}");
    }

    @Test
    public void testMaskResponseBody() {
        String input =
                "{\"name\":\"Steve\",\n" +
                        "\"list\":[\n" +
                        "    {\"name\": \"Nick\"},\n" +
                        "    {\n" +
                        "        \"name\": \"Wen\",\n" +
                        "        \"accounts\": [\"1\", \"2\", \"3\"]\n" +
                        "    },\n" +
                        "    {\n" +
                        "        \"name\": \"Steve\",\n" +
                        "        \"accounts\": [\"4\", \"5\", \"666666\"]\n" +
                        "    },\n" +
                        "    \"secret1\", \"secret2\"],\n" +
                        "\"list1\": [\"1\", \"333\", \"55555\"],\n" +
                        "\"password\":\"secret\"}";
//        String input = "{\"name\":\"Steve\",\"list\":[\"secret1\", \"secret2\"],\"password\":\"secret\"}";
        String output = Mask.maskJson(input, "test2");
        System.out.println(output);
        Assertions.assertEquals(JsonPath.parse(output).read("$.list[2].accounts[2]"), "******");
        Assertions.assertEquals(JsonPath.parse(output).read("$.list[1].name"), "***");
        Assertions.assertEquals(JsonPath.parse(output).read("$.list1[2]"), "*****");
        Assertions.assertEquals(JsonPath.parse(output).read("$.password"), "******");
    }

    @Test
    public void testMaskIssue942()
    {
        String input = "{\"name\":\"Steve\", \"list\":[{\"name\":\"Josh\", \"creditCardNumber\":\"4586996854721123\"}],\"password\":\"secret\"}";
        String output = Mask.maskJson(input, "testIssue942");
        System.out.println(output);
        Assertions.assertEquals(output, "{\"name\":\"Steve\",\"list\":[{\"name\":\"Josh\",\"creditCardNumber\":\"****************\"}],\"password\":\"secret\"}");
    }

    @Test
    public void testNullInput()
    {
        String output;

        // Mask.maskString((String) null, "");
        output = Mask.maskString((String) null, "uri");
        System.out.println("ouput = " + output);
        Assertions.assertEquals(null, output);

        // Mask.maskRegex((String) null, "", "");
        output = Mask.maskRegex((String) null, "queryParameter", "accountNo");
        System.out.println("output = " + output);
        Assertions.assertEquals(null, output);

        // Mask.maskJson((String) null,"key");
        output = Mask.maskJson((String) null, "uri");
        System.out.println("output = " + output);
        Assertions.assertEquals(null, output);

        // Mask.maskJson((InputStream) null, "");
        output = Mask.maskJson((InputStream) null, "uri");
        System.out.println("output = " + output);
        Assertions.assertEquals(null, output);

        // Mask.maskJson((Object) null, "");
        output = Mask.maskJson((Object) null, "uri");
        System.out.println("output = " + output);
        Assertions.assertEquals(null, output);

        // Mask.maskJson((DocumentContext) null, "");
        output = Mask.maskJson((DocumentContext) null, "uri");
        System.out.println("output = " + output);
        Assertions.assertEquals(null, output);
    }

    @Test
    public void testMaskNonStringScalars() {
        String input = "{\"accountNumber\":1234567890123456,\"balance\":1234.56,\"verified\":true}";
        String output = Mask.maskJson(input, "testScalars");
        System.out.println(output);
        Assertions.assertEquals("****************", JsonPath.parse(output).read("$.accountNumber"));
        Assertions.assertEquals("*******", JsonPath.parse(output).read("$.balance"));
        Assertions.assertEquals("****", JsonPath.parse(output).read("$.verified"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMaskParsedMapBody() {
        Map<String, Object> contact = new HashMap<>();
        contact.put("phone", "416-111-1111");
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Steve");
        body.put("contact", contact);
        body.put("password", "secret");

        Object output = Mask.maskObject(body, "test1");
        System.out.println(output);
        Assertions.assertTrue(output instanceof Map);
        Map<String, Object> masked = (Map<String, Object>) output;
        Assertions.assertEquals("Steve", masked.get("name"));
        Assertions.assertEquals("******", masked.get("password"));
        Assertions.assertEquals("************", ((Map<String, Object>) masked.get("contact")).get("phone"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMaskParsedListBody() {
        Map<String, Object> element = new HashMap<>();
        element.put("name", "Josh");
        element.put("creditCardNumber", "4586996854721123");
        List<Object> body = new ArrayList<>();
        body.add(element);

        Object output = Mask.maskObject(body, "testListBody");
        System.out.println(output);
        Assertions.assertTrue(output instanceof List);
        Map<String, Object> masked = (Map<String, Object>) ((List<Object>) output).get(0);
        Assertions.assertEquals("Josh", masked.get("name"));
        Assertions.assertEquals("****************", masked.get("creditCardNumber"));
    }

    @Test
    public void testMaskObjectLeavesOtherInputAlone() {
        Assertions.assertNull(Mask.maskObject(null, "test1"));
        Assertions.assertEquals("plain text", Mask.maskObject("plain text", "test1"));
    }
}
