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
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.EnumSet;
import java.util.Set;

public class MaskTest {

    @BeforeClass
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
        Assert.assertEquals("/v1/customer?sin=masked&password=******&number=----------------", output);
    }

    @Test
    public void testMaskQueryParameter() {
        String test = "aaaa";
        String output = Mask.maskRegex(test, "queryParameter", "accountNo");
        System.out.println("output = " + output);
        Assert.assertEquals(output, "****");
    }

    @Test
    public void testMaskRequestHeader() {
        String testHeader1 = "test";
        String testHeader2 = "tests";
        String output1 = Mask.maskRegex(testHeader1, "requestHeader", "header1");
        System.out.println("output1 = " + output1);
        Assert.assertEquals(output1, "****");
        String output2 = Mask.maskRegex(testHeader2, "requestHeader", "header2");
        Assert.assertEquals(output2, "*****");
    }

    @Test
    public void testMaskResponseHeader() {
        String testHeader = "header";
        String output = Mask.maskRegex(testHeader, "responseHeader", "header3");
        System.out.println("output = " + output);
        Assert.assertEquals(output, "******");
    }
    @Test
    public void testMaskRequestBody() {
        String input = "{\"name\":\"Steve\",\"contact\":{\"phone\":\"416-111-1111\"},\"password\":\"secret\"}";
        String output = Mask.maskJson(input, "test1");
        System.out.println(output);
        Assert.assertEquals(JsonPath.parse(output).read("$.contact.phone"), "************");
        Assert.assertEquals(JsonPath.parse(output).read("$.password"), "******");
        Assert.assertEquals(output, "{\"name\":\"Steve\",\"contact\":{\"phone\":\"************\"},\"password\":\"******\"}");
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
        Assert.assertEquals(JsonPath.parse(output).read("$.list[2].accounts[2]"), "******");
        Assert.assertEquals(JsonPath.parse(output).read("$.list[1].name"), "***");
        Assert.assertEquals(JsonPath.parse(output).read("$.list1[2]"), "*****");
        Assert.assertEquals(JsonPath.parse(output).read("$.password"), "******");
    }
}
