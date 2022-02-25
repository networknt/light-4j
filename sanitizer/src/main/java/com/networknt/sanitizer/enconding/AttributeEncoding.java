package com.networknt.sanitizer.enconding;

import org.owasp.encoder.Encode;

public class AttributeEncoding implements Encoding {
    @Override
    public String getId() {
        return "javascript-attribute";
    }

    @Override
    public String apply(String data) {
        return Encode.forJavaScriptAttribute(data);
    }
}
