package com.networknt.sanitizer.enconding;

import org.owasp.encoder.Encode;

public class GeneralEncoding implements Encoding {

    @Override
    public String getId() {
        return "javascript";
    }

    @Override
    public String apply(String data) {
        return Encode.forJavaScript(data);
    }
}
