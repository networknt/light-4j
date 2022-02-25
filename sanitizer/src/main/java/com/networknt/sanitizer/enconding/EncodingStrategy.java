package com.networknt.sanitizer.enconding;

public class EncodingStrategy {
    public static Encoding of(String value) {

        if (value == null) {
            return new SourceEncoding();
        }
        switch (value) {
            case "javascript":
                return new GeneralEncoding();
            case "javascript-source":
                return new SourceEncoding();
            case "javascript-attribute":
                return new AttributeEncoding();
            case "javascript-block":
                return new BlockEncoding();
            default:
                throw new IllegalStateException(String.format("Encoding unknown: %s", value));
        }
    }
}
