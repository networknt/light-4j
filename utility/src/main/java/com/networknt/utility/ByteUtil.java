package com.networknt.utility;

import java.nio.ByteBuffer;
import java.util.Random;

/**
 * Byte utility class.
 */
public class ByteUtil {
    private ByteUtil() {
    }

    /**
     * Converts a long to a byte array.
     * @param x long value
     * @return byte array
     */
    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(0, x);
        return buffer.array();
    }

    /**
     * Converts a byte array to a long.
     * @param bytes byte array
     * @return long value
     */
    public static long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(bytes, 0, bytes.length);
        buffer.flip();//need flip
        return buffer.getLong();
    }

    /**
     * Generates a random alphabetic string.
     * @param length length of the string
     * @return String random alphabetic string
     */
    public static String randomAlphabetic(int length) {
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        Random random = new Random();

        return random.ints(leftLimit, rightLimit + 1)
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    /**
     * Generates a random numeric string.
     * @param length length of the string
     * @return String random numeric string
     */
    public static String randomNumeric(int length) {
        int leftLimit = 48; // letter '0'
        int rightLimit = 57; // letter '9'
        Random random = new Random();

        return random.ints(leftLimit, rightLimit + 1)
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

}
