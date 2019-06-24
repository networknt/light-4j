package com.networknt.sanitizer;

import com.networknt.sanitizer.enconding.Encoding;

public class FakeEncoding implements Encoding {

    @Override
    public String getId() {
        return "fake";
    }

    @Override
    public String apply(String data) {
        return data.replace("alert", "example");
    }
}