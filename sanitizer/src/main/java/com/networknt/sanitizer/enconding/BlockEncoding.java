package com.networknt.sanitizer.enconding;

import org.owasp.encoder.Encode;

public class BlockEncoding implements Encoding {
    @Override
    public String getId() {
        return "javascript-block";
    }

    @Override
    public String apply(String data) {
        return Encode.forJavaScriptBlock(data);
    }
}
