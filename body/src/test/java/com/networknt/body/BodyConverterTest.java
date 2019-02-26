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

        List<Object> aConvertedListvalue = (List<Object>) bodyMap.get(aKey);
        Assert.assertEquals(1, aConvertedListvalue.size());
        Assert.assertTrue(aConvertedListvalue.get(0) instanceof FormData.FormValue);
        Assert.assertEquals(aValue, ((FormData.FormValue)aConvertedListvalue.get(0)).getValue());

        List<Object> anotherListvalues = (List<Object>) bodyMap.get(anotherKey);
        Assert.assertEquals(1, anotherListvalues.size());
        Assert.assertTrue(anotherListvalues.get(0) instanceof FormData.FormValue);
        Assert.assertEquals(anotherValue, ((FormData.FormValue)anotherListvalues.get(0)).getValue());
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

        Assert.assertTrue(aConvertedListvalue.get(0) instanceof FormData.FormValue);
        Assert.assertEquals(aValue, ((FormData.FormValue)aConvertedListvalue.get(0)).getValue());

        Assert.assertTrue(aConvertedListvalue.get(1) instanceof FormData.FormValue);
        Assert.assertEquals(anotherValue, ((FormData.FormValue)aConvertedListvalue.get(1)).getValue());
    }
}