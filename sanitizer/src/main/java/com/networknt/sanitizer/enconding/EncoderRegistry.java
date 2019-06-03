package com.networknt.sanitizer.enconding;


public class EncoderRegistry {

    private static Encoding encoding;

    public static void registry(Encoding encoding) {
        EncoderRegistry.encoding = encoding;
    }

    public static boolean hasEncodingsRegistered() {
        return encoding != null;
    }

    public static Encoding getEncoding() {
        return encoding;
    }

    public static void reset() {
        encoding = null;
    }
}
