package com.networknt.sanitizer.enconding;

public class EncodingStrategy {

    public static Encoding of(String value) {
        if (EncoderRegistry.hasEncodingsRegistered()) {
            if (EncoderRegistry.getEncoding().getId().equals(value)) {
                return EncoderRegistry.getEncoding();
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
