package com.networknt.sanitizer;

import com.networknt.sanitizer.enconding.Encoder;
import com.networknt.sanitizer.enconding.Encoding;
import org.junit.Test;
import org.mockito.Mockito;
import org.owasp.encoder.Encode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class EncoderTest {
    private String s = "A'NDERSONoô";
    private String value = "anyValue";

    @Test
    public void shouldApplyEncodingInValueStrings() {
        testEncodingFor(createMapOfString());
    }

    private Map<String, Object> createMapOfString() {
        Map<String, Object> mapOfString = new HashMap<>();
        mapOfString.put("data", value);
        return mapOfString;
    }

    @Test
    public void shouldApplyEncodingInMapOfMapInYoursValues() {
        testEncodingFor(createMapOfMap());
    }

    private Map<String, Object> createMapOfMap() {
        Map<String, Object> mapOfMap = new HashMap<>();
        mapOfMap.put("parent", createMapOfString());
        return mapOfMap;
    }

    @Test
    public void shouldApplyEncodingInMapOfListInYoursElements() {
        testEncodingFor(createMapOfList());
    }

    private Map<String, Object> createMapOfList() {
        Map<String, Object> mapOfList = new HashMap<>();
        mapOfList.put("data", Arrays.asList(value));

        return mapOfList;
    }

    @Test
    public void shouldApplyEncodingInMapOfListOfMapInYoursValues() {
        testEncodingFor(createMapOfListOfMap());
    }

    private Map<String, Object> createMapOfListOfMap() {
        Map<String, Object> mapOfListOfMap = new HashMap<>();

        mapOfListOfMap.put("parent", Arrays.asList(createMapOfString()));
        return mapOfListOfMap;
    }

    @Test
    public void shouldApplyEncodingInMapOfListOfList() {
        testEncodingFor(createMapOfListOfList());
    }

    private Map<String, Object> createMapOfListOfList() {
        Map<String, Object> mapOfListOfList = new HashMap<>();

        mapOfListOfList.put("data", Arrays.asList(Arrays.asList(value)));
        return mapOfListOfList;
    }

    @Test
    public void shouldIgnorePropertyIfItIsInPropertiesToIgnoreList() {
        Map<String, Object> map = new HashMap<>();
        map.put("data1", "value1");
        map.put("data2", "value2");
        map.put("data3", "value3");
        map.put("data4", "value4");
        Encoding encoding = new FakeEncoding();
        Encoder encoder = Mockito.spy(new Encoder(encoding, Arrays.asList("data2", "data4"), null));

        encoder.encodeNode(map);

        Mockito.verify(encoder).applyEncoding("value1");
        Mockito.verify(encoder).applyEncoding("value3");
        Mockito.verify(encoder, Mockito.never()).applyEncoding("value2");
        Mockito.verify(encoder, Mockito.never()).applyEncoding("value4");
        Mockito.verify(encoder, Mockito.times(2)).applyEncoding(Mockito.any());
    }

    @Test
    public void shouldApplyEncodingIfAttributesToAppreciateIsNotEmpty() {
        Map<String, Object> map = new HashMap<>();
        map.put("data1", "value1");
        map.put("data2", "value2");
        map.put("data3", "value3");
        map.put("data4", "value4");
        Encoding encoding = new FakeEncoding();
        Encoder encoder = Mockito.spy(new Encoder(encoding, null, Arrays.asList("data3")));

        encoder.encodeNode(map);

        Mockito.verify(encoder, Mockito.never()).applyEncoding("value1");
        Mockito.verify(encoder).applyEncoding("value3");
        Mockito.verify(encoder, Mockito.never()).applyEncoding("value2");
        Mockito.verify(encoder, Mockito.never()).applyEncoding("value4");
    }

    @Test
    public void shouldIgnoreListOfAttributesToIgnoreIfThereIsAListOfAttributesToAppreciate() {
        Map<String, Object> map = new HashMap<>();
        map.put("data1", "value1");
        map.put("data2", "value2");
        map.put("data3", "value3");
        map.put("data4", "value4");
        Encoding encoding = new FakeEncoding();
        Encoder encoder = Mockito.spy(new Encoder(encoding, Arrays.asList("data2"), Arrays.asList("data3")));

        encoder.encodeNode(map);

        Mockito.verify(encoder, Mockito.never()).applyEncoding("value1");
        Mockito.verify(encoder).applyEncoding("value3");
        Mockito.verify(encoder, Mockito.never()).applyEncoding("value2");
        Mockito.verify(encoder, Mockito.never()).applyEncoding("value4");
    }

    @Test
    public void testForJavaScript() {
        String result = Encode.forJavaScript(s);
        System.out.println("JavaScript: " + result);
    }

    @Test
    public void testForJavaScriptAttribute() {
        String result = Encode.forJavaScriptAttribute(s);
        System.out.println("JavaScriptAttribute: " + result);
    }

    @Test
    public void testForJavaScriptBlock() {
        String result = Encode.forJavaScriptBlock(s);
        System.out.println("JavaScriptBlock: " + result);
    }

    @Test
    public void testForJavaScriptSource() {
        String result = Encode.forJavaScriptSource(s);
        System.out.println("JavaScriptSource: " + result);
    }

    private void testEncodingFor(Map<String, Object> structure) {
        Encoding encoding = new FakeEncoding();
        Encoder encoder = Mockito.spy(new Encoder(encoding, null, null));

        encoder.encodeNode(structure);

        Mockito.verify(encoder).applyEncoding(value);
        Mockito.verify(encoder).applyEncoding(Mockito.any());
    }
}
