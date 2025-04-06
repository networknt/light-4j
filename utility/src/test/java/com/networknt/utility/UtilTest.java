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

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UtilTest {
    @Test
    public void testGetUUID() {
        String id0 = Util.getUUID();
        String id1 = Util.getUUID();
        String id2 = Util.getUUID();
        String id3 = Util.getUUID();
        String id4 = Util.getUUID();
        String id5 = Util.getUUID();
        String id6 = Util.getUUID();
        String id7 = Util.getUUID();
        String id8 = Util.getUUID();
        String id9 = Util.getUUID();
        System.out.println("uuid = " + id0);
        System.out.println("uuid = " + id1);
        System.out.println("uuid = " + id2);
        System.out.println("uuid = " + id3);
        System.out.println("uuid = " + id4);
        System.out.println("uuid = " + id5);
        System.out.println("uuid = " + id6);
        System.out.println("uuid = " + id7);
        System.out.println("uuid = " + id8);
        System.out.println("uuid = " + id9);
        Assert.assertNotEquals(id1, id2);
    }

    @Test
    public void testGetCleanUUID() {
        List<String> validUUIDs = new ArrayList<>();

        // Generate UUIDs until we have 10 without "-" or "_"
        while (validUUIDs.size() < 10) {
            String uuid = Util.getUUID();
            if (!uuid.contains("-") && !uuid.contains("_")) {
                validUUIDs.add(uuid);
            }
        }

        // Assign to individual variables
        String id0 = validUUIDs.get(0);
        String id1 = validUUIDs.get(1);
        String id2 = validUUIDs.get(2);
        String id3 = validUUIDs.get(3);
        String id4 = validUUIDs.get(4);
        String id5 = validUUIDs.get(5);
        String id6 = validUUIDs.get(6);
        String id7 = validUUIDs.get(7);
        String id8 = validUUIDs.get(8);
        String id9 = validUUIDs.get(9);

        // Print results
        System.out.println("uuid = " + id0);
        System.out.println("uuid = " + id1);
        System.out.println("uuid = " + id2);
        System.out.println("uuid = " + id3);
        System.out.println("uuid = " + id4);
        System.out.println("uuid = " + id5);
        System.out.println("uuid = " + id6);
        System.out.println("uuid = " + id7);
        System.out.println("uuid = " + id8);
        System.out.println("uuid = " + id9);

        Assert.assertNotEquals(id1, id2);
    }

    @Test
    public void testGetJarVersion() {
        String ver = Util.getJarVersion();
        System.out.println("ver =" + ver);
    }

    @Test
    public void testSubVariables() {
        Map<String, String> variables = new HashMap<>();
        variables.put("v1", "abc");
        variables.put("v2", "def");

        String text = "This is a test for ${v1} and ${v2}";
        String expect = "This is a test for abc and def";

        Assert.assertEquals(expect, Util.substituteVariables(text, variables));

    }

    @Test
    public void testParseAttributes() {
        String attributesString = "department^=^Engineering~location^=^New York City~project^=^Project Alpha";
        Map<String, String> parsedAttributes = Util.parseAttributes(attributesString);
        System.out.println("Parsed Attributes: " + parsedAttributes);
        // Expected Output: Parsed Attributes: {location=New York City, project=Project Alpha, department=Engineering}

        String attributesStringWithNoSpace = "department^=^Engineering~location^=^NewYorkCity";
        Map<String, String> parsedAttributes2 = Util.parseAttributes(attributesStringWithNoSpace);
        System.out.println("Parsed Attributes: " + parsedAttributes2);
        // Expected Output: Parsed Attributes: {location=NewYorkCity, department=Engineering}

        String emptyAttributesString = null;
        Map<String, String> parsedAttributes3 = Util.parseAttributes(emptyAttributesString);
        System.out.println("Parsed Attributes: " + parsedAttributes3);
        // Expected Output: Parsed Attributes: {}

        String attributesStringWithEmptyValue = "department^=^~location^=^New York City";
        Map<String, String> parsedAttributes4 = Util.parseAttributes(attributesStringWithEmptyValue);
        System.out.println("Parsed Attributes: " + parsedAttributes4);
        // Expected Output: Parsed Attributes: {location=New York City, department=}

    }
}
