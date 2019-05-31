package com.networknt.sanitizer.enconding;

public class EncodingStrategy {

    public static Encoding of(String value) {
        if (EncodingRegistry.hasEncodingsRegistered()) {
            if (EncodingRegistry.getEncoding().getId().equals(value)) {
                return EncodingRegistry.getEncoding();
            }
        }

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
