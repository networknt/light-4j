/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.utility;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Utility to calculate hash values.
 *
 * @author Steve Hu
 */
public class HashUtil {

    private HashUtil() {throw new UnsupportedOperationException("do not instantiate");}

    /**
     * Calculates a SHA-256 hash of an input string.
     * @param input String
     * @return String SHA-256 hash
     */
    @Deprecated
    public static String md5(String input) {
        return sha256(input);
    }

    /**
     * Calculates a SHA-256 hash of an input string.
     * @param input String
     * @return String SHA-256 hash
     */
    public static String sha256(String input) {
        if(null == input) return null;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return hex(digest.digest(input.getBytes(UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Converts a byte array to a hex string.
     * @param array byte array
     * @return String hex string
     */
    public static String hex(byte[] array) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < array.length; ++i) {
            sb.append(Integer.toHexString((array[i]
                    & 0xFF) | 0x100).substring(1,3));
        }
        return sb.toString();
    }
    /**
     * Calculates a SHA-256 hash in hex format.
     * @param message String
     * @return String hex SHA-256 hash
     */
    @Deprecated
    public static String md5Hex (String message) {
        return sha256Hex(message);
    }

    /**
     * Calculates a SHA-256 hash in hex format.
     * @param message String
     * @return String hex SHA-256 hash
     */
    public static String sha256Hex (String message) {
        if(message == null) return null;
        try {
            MessageDigest md =
                    MessageDigest.getInstance("SHA-256");
            return hex (md.digest(message.getBytes(UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates a strong password hash.
     * @param password String
     * @return String password hash
     * @throws NoSuchAlgorithmException if algorithm not found
     * @throws InvalidKeySpecException if key spec is invalid
     */
    public static String generateStrongPasswordHash(String password) throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        int iterations = 1000;
        char[] chars = password.toCharArray();
        byte[] salt = getSalt().getBytes(UTF_8);

        PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, 64 * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        return iterations + ":" + toHex(salt) + ":" + toHex(hash);
    }

    private static String getSalt() throws NoSuchAlgorithmException
    {
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[16];
        sr.nextBytes(salt);
        return Arrays.toString(salt);
    }

    private static String toHex(byte[] array) throws NoSuchAlgorithmException
    {
        BigInteger bi = new BigInteger(1, array);
        String hex = bi.toString(16);
        int paddingLength = (array.length * 2) - hex.length();
        if(paddingLength > 0)
        {
            return String.format("%0"  +paddingLength + "d", 0) + hex;
        }else{
            return hex;
        }
    }

    /**
     * Validates a password against a stored hash.
     * @param originalPassword char array
     * @param storedPassword String stored hash
     * @return boolean true if valid
     * @throws NoSuchAlgorithmException if algorithm not found
     * @throws InvalidKeySpecException if key spec is invalid
     */
    public static boolean validatePassword(char[] originalPassword, String storedPassword) throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        String[] parts = storedPassword.split(":");
        int iterations = Integer.parseInt(parts[0]);
        byte[] salt = fromHex(parts[1]);
        byte[] hash = fromHex(parts[2]);

        PBEKeySpec spec = new PBEKeySpec(originalPassword, salt, iterations, hash.length * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] testHash = skf.generateSecret(spec).getEncoded();

        int diff = hash.length ^ testHash.length;
        for(int i = 0; i < hash.length && i < testHash.length; i++)
        {
            diff |= hash[i] ^ testHash[i];
        }
        return diff == 0;
    }

    /**
     * Converts a hex string to a byte array.
     * @param hex String hex
     * @return byte array
     * @throws NoSuchAlgorithmException if algorithm not found
     */
    private static byte[] fromHex(String hex) throws NoSuchAlgorithmException
    {
        byte[] bytes = new byte[hex.length() / 2];
        for(int i = 0; i<bytes.length ;i++)
        {
            bytes[i] = (byte)Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }

}
