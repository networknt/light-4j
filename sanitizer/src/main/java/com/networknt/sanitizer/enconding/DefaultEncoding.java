package com.networknt.sanitizer.enconding;

import org.owasp.encoder.Encode;

public class DefaultEncoding implements Encoding {

    @Override
    public String getId() {
        return "default";
    }

    @Override
    public String apply(String data) {
        return Encode.forJavaScriptSource(data);
    }
}
