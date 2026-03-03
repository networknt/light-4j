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

package com.networknt.utility;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class StringUtilsTest {
    @Test
    @Disabled
    public void testExpandEnvVars() {
        String s = "IP=${DOCKER_HOST_IP}";
        Assertions.assertEquals("IP=192.168.1.120", StringUtils.expandEnvVars(s));
    }

    @Test
    public void testInputStreamToString_withExpected() throws IOException {
        String expected = "test data";
        InputStream anyInputStream = new ByteArrayInputStream(expected.getBytes(StandardCharsets.UTF_8));
        String actual = StringUtils.inputStreamToString(anyInputStream, StandardCharsets.UTF_8);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testIsJwtToken() {
        String jwtBearer = "Bearer eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTk0MTEyMjc3MiwianRpIjoiTkc4NWdVOFR0SEZuSThkS2JsQnBTUSIsImlhdCI6MTYyNTc2Mjc3MiwibmJmIjoxNjI1NzYyNjUyLCJ2ZXJzaW9uIjoiMS4wIiwiY2xpZW50X2lkIjoiZjdkNDIzNDgtYzY0Ny00ZWZiLWE1MmQtNGM1Nzg3NDIxZTcyIiwic2NvcGUiOiJwb3J0YWwuciBwb3J0YWwudyIsInNlcnZpY2UiOiIwMTAwIn0.Q6BN5CGZL2fBWJk4PIlfSNXpnVyFhK6H8X4caKqxE1XAbX5UieCdXazCuwZ15wxyQJgWCsv4efoiwO12apGVEPxIc7gpvctPrRIDo59dmTjfWH0p3ja0Zp8tYLD-5Sh65WUtJtkvPQk0uG96JJ64Da28lU4lGFZaCvkaS-Et9Wn0BxrlCE5_ta66Qc9t4iUMeAsAHIZJffOBsREFhOpC0dKSXBAyt9yuLDuDt9j7HURXBHyxSBrv8Nj_JIXvKhAxquffwjZF7IBqb3QRr-sJV0auy-aBQ1v8dYuEyIawmIP5108LH8QdH-K8NkI1wMnNOz_wWDgixOcQqERmoQ_Q3g";
        String jwt = "eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTk0MTEyMjc3MiwianRpIjoiTkc4NWdVOFR0SEZuSThkS2JsQnBTUSIsImlhdCI6MTYyNTc2Mjc3MiwibmJmIjoxNjI1NzYyNjUyLCJ2ZXJzaW9uIjoiMS4wIiwiY2xpZW50X2lkIjoiZjdkNDIzNDgtYzY0Ny00ZWZiLWE1MmQtNGM1Nzg3NDIxZTcyIiwic2NvcGUiOiJwb3J0YWwuciBwb3J0YWwudyIsInNlcnZpY2UiOiIwMTAwIn0.Q6BN5CGZL2fBWJk4PIlfSNXpnVyFhK6H8X4caKqxE1XAbX5UieCdXazCuwZ15wxyQJgWCsv4efoiwO12apGVEPxIc7gpvctPrRIDo59dmTjfWH0p3ja0Zp8tYLD-5Sh65WUtJtkvPQk0uG96JJ64Da28lU4lGFZaCvkaS-Et9Wn0BxrlCE5_ta66Qc9t4iUMeAsAHIZJffOBsREFhOpC0dKSXBAyt9yuLDuDt9j7HURXBHyxSBrv8Nj_JIXvKhAxquffwjZF7IBqb3QRr-sJV0auy-aBQ1v8dYuEyIawmIP5108LH8QdH-K8NkI1wMnNOz_wWDgixOcQqERmoQ_Q3g";
        String swt = "4VHHkOUfQQuInU5auCHFvw";

        Assertions.assertTrue(StringUtils.isJwtToken(jwtBearer));
        Assertions.assertTrue(StringUtils.isJwtToken(jwt));
        Assertions.assertFalse(StringUtils.isJwtToken(swt));
    }

    @Test
    public void testMaskHalfString() {
        String s = "1234567890";
        Assertions.assertEquals("*****67890", StringUtils.maskHalfString(s));
        s = "123456789";
        Assertions.assertEquals("****56789", StringUtils.maskHalfString(s));
    }

    @Test
    public void testMatchPath() {
        String pattern = "/v1/pets/{petId}";
        String path = "/v1/pets/1";
        Assertions.assertTrue(StringUtils.matchPathToPattern(path, pattern));
        pattern = "/v1/pets/{petId}/name";
        path = "/v1/pets/1/name";
        Assertions.assertTrue(StringUtils.matchPathToPattern(path, pattern));
        pattern = "/v1/pets/{petId}";
        Assertions.assertTrue(StringUtils.matchPathToPattern(path, pattern));

        pattern = "/foo/bar";
        Assertions.assertTrue(StringUtils.matchPathToPattern("/foo/bar", pattern));
        Assertions.assertFalse(StringUtils.matchPathToPattern("/foo/bar?abc=123", pattern));

        pattern = "/gateway/dev/ph-l4j-files/file?version=1";
        Assertions.assertFalse(StringUtils.matchPathToPattern("/dev-ph-l4j-files/file?version=1", pattern));

        pattern = "/gateway/dev/ph-l4j-files/file?version=1";
        Assertions.assertTrue(StringUtils.matchPathToPattern("/gateway/dev/ph-l4j-files/file?version=1", pattern));

        pattern = "/gateway/dev/ph-l4j-files/file/05048267?version=1";
        Assertions.assertFalse(StringUtils.matchPathToPattern("/gateway/dev/ph-l4j-files/file?version=1", pattern));
    }

    @Test
    public void testGetSecondPart() {
        String result = StringUtils.getSecondPart("Hello World");
        Assertions.assertEquals("World", result);

        result = StringUtils.getSecondPart("NoSpace");
        Assertions.assertNull(result);

        result = StringUtils.getSecondPart("Multiple Words Here");
        Assertions.assertEquals("Words Here", result);

        result = StringUtils.getSecondPart("Hello World ");
        Assertions.assertEquals("World ", result);

        result = StringUtils.getSecondPart("Hello  World ");
        Assertions.assertEquals(" World ", result);
    }

    @Test
    public void testJsonExtraBackSlash() {
        // String jsonStringWithBackslashes =
        // """
        // {\n  \"$schema\": \"http://json-schema.org/draft-07/schema\",\n  \"$id\": \"http://example.com/example.json\",\n  \"type\": \"object\",\n  \"required\": [\n    \"extra_vars\"\n  ],\n  \"properties\": {\n    \"extra_vars\": {\n      \"type\": \"object\",\n      \"required\": [\n        \"ev_whs_env\",\n        \"ev_whs_env\",\n        \"ev_targets\",\n        \"ev_action\",\n        \"ev_pre_validation_check\",\n        \"ev_api_jvm_name\",\n        \"ev_productVersion\",\n        \"ev_productId\",\n        \"ev_tag\",\n        \"ev_apiId\",\n        \"ev_api_version\"\n      ],\n      \"properties\": {\n        \"ev_chg_ticket_number\": {\n          \"type\": \"string\",\n          \"default\": \"dev\"\n        },\n        \"ev_whs_env\": {\n          \"type\": \"string\",\n          \"default\": \"dev\"\n        },\n        \"ev_targets\": {\n          \"type\": \"string\"\n        },\n        \"ev_action\": {\n          \"type\": \"string\"\n        },\n        \"ev_pre_validation_check\": {\n          \"type\": \"string\",\n          \"default\": \"no\"\n        },\n        \"ev_api_jvm_name\": {\n          \"type\": \"string\"\n        },\n        \"ev_productVersion\": {\n          \"type\": \"string\"\n        },\n        \"ev_productId\": {\n          \"type\": \"string\"\n        },\n        \"ev_tag\": {\n          \"type\": \"string\"\n        },\n        \"ev_apiId\": {\n          \"type\": \"string\"\n        },\n        \"ev_api_version\": {\n          \"type\": \"string\"\n        }\n      }\n    }\n  }\n}\n
        // """;
        String jsonStringWithBackslashes =
        """
        {\n  \"$schema\": \"http://json-schema.org/draft-07/schema\",\n  \"$id\": \"http://example.com/example.json\",\n  \"type\": \"object\",\n  \"required\": [\n    \"job\"\n  ],\n  \"properties\": {\n    \"job\": {\n      \"type\": \"integer\"\n    },\n    \"ignored_fields\": {\n      \"type\": \"object\",\n      \"additionalProperties\": {}\n    }\n  }\n}
        """;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            // Parse the JSON string into a Java object
            Object jsonObject = objectMapper.readValue(jsonStringWithBackslashes, Object.class);
            // Re-serialize the Java object into a JSON string
            String cleanJsonString = objectMapper.writeValueAsString(jsonObject);
            System.out.println(cleanJsonString); // Output: {"key":"value with \"escaped quote\" and \\ backslash"}
        } catch (Exception e) {
            e.printStackTrace();
        }
        //{"$schema":"http://json-schema.org/draft-07/schema","$id":"http://example.com/example.json","type":"object","required":["extra_vars"],"properties":{"extra_vars":{"type":"object","required":["ev_whs_env","ev_whs_env","ev_targets","ev_action","ev_pre_validation_check","ev_api_jvm_name","ev_productVersion","ev_productId","ev_tag","ev_apiId","ev_api_version"],"properties":{"ev_chg_ticket_number":{"type":"string","default":"dev"},"ev_whs_env":{"type":"string","default":"dev"},"ev_targets":{"type":"string"},"ev_action":{"type":"string"},"ev_pre_validation_check":{"type":"string","default":"no"},"ev_api_jvm_name":{"type":"string"},"ev_productVersion":{"type":"string"},"ev_productId":{"type":"string"},"ev_tag":{"type":"string"},"ev_apiId":{"type":"string"},"ev_api_version":{"type":"string"}}}}}
        //{"$schema":"http://json-schema.org/draft-07/schema","$id":"http://example.com/example.json","type":"object","required":["job"],"properties":{"job":{"type":"integer"},"ignored_fields":{"type":"object","additionalProperties":{}}}}
    }
}
