/*
 * Copyright (c) 2019 Network New Technologies Inc.
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
package com.networknt.body;

import io.undertow.server.handlers.form.FormData;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class BodyConverterTest {

    @Test
    public void shouldToGetEmptyMapWhenFormDataIsEmpty() {
        FormData formData = new FormData(99);
        Map<String, Object> bodyMap = BodyConverter.convert(formData);

        Assert.assertEquals(0, bodyMap.size());
    }

    @Test
    public void shouldToGetConvertedFormDataInAMap() {
        String aKey = "aKey";
        String aValue = "aValue";
        String anotherKey = "anotherKey";
        String anotherValue = "anotherValue";

        FormData formData = new FormData(99);
        formData.add(aKey, aValue);
        formData.add(anotherKey, anotherValue);

        Map<String, Object> bodyMap = BodyConverter.convert(formData);

        Assert.assertEquals(2, bodyMap.size());

        Object aConvertedListvalue = bodyMap.get(aKey);
        Assert.assertTrue(aConvertedListvalue instanceof String);
        Assert.assertEquals(aValue, aConvertedListvalue);

        Object anotherListvalues = bodyMap.get(anotherKey);
        Assert.assertTrue(anotherListvalues instanceof String);
        Assert.assertEquals(anotherValue, anotherListvalues);
    }

    @Test
    public void shouldToGetConvertedFormDataInAMapGroupedByKey() {
        String aKey = "aKey";
        String aValue = "aValue";
        String anotherValue = "anotherValue";

        FormData formData = new FormData(99);
        formData.add(aKey, aValue);
        formData.add(aKey, anotherValue);

        Map<String, Object> bodyMap = BodyConverter.convert(formData);

        Assert.assertEquals(1, bodyMap.size());

        List<Object> aConvertedListvalue = (List<Object>) bodyMap.get(aKey);
        Assert.assertEquals(2, aConvertedListvalue.size());

        Assert.assertTrue(aConvertedListvalue.get(0) instanceof String);
        Assert.assertEquals(aValue, aConvertedListvalue.get(0));

        Assert.assertTrue(aConvertedListvalue.get(1) instanceof String);
        Assert.assertEquals(anotherValue, aConvertedListvalue.get(1));
    }
}