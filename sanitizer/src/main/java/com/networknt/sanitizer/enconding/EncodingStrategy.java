package com.networknt.sanitizer.enconding;

public class EncodingStrategy {

    public static Encoding of(String value) {
        if (value == null) {
            return new DefaultEncoding();
        }
        switch (value) {
            case "default":
                return new DefaultEncoding();
            default:
                throw new IllegalStateException(String.format("Encoding unknown: %s", value));
        }
    }
}
