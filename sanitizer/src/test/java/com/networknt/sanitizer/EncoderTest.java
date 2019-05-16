package com.networknt.sanitizer;

import org.junit.Test;
import org.owasp.encoder.Encode;

public class EncoderTest {
    String s = "A'NDERSONoô";

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

}
