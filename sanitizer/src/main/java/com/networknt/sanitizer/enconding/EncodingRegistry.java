package com.networknt.sanitizer.enconding;


public class EncodingRegistry {

    private static Encoding encoding;

    public static void registry(Encoding encoding) {
        EncodingRegistry.encoding = encoding;
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
