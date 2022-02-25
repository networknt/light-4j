package com.networknt.sanitizer.enconding;

import org.owasp.encoder.Encode;

public class SourceEncoding implements Encoding {
    @Override
    public String getId() {
        return "javascript-source";
    }

    @Override
    public String apply(String data) {
        return Encode.forJavaScriptSource(data);
    }
}
