package com.networknt.sanitizer.enconding;

public class EncodingStrategy {

    public static Encoding of(String value) {
        return new DefaultEncoding();
    }
}
