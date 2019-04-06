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

package com.networknt.decrypt;

import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class AESDecryptor implements Decryptor {

    private static final int ITERATIONS = 65536;

    private static final String STRING_ENCODING = "UTF-8";

    /**
     * If we user Key size of 256 we will get java.security.InvalidKeyException:
     * Illegal key size or default parameters , Unless we configure Java
     * Cryptography Extension 128
     */
    private static final int KEY_SIZE = 128;

    private static final byte[] SALT = { (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0 };

    private SecretKeySpec secret;

    private Cipher cipher;

    private Base64.Decoder base64Decoder = Base64.getDecoder();

    public AESDecryptor() {
        try {
            /* Derive the key, given password and salt. */
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec;

            spec = new PBEKeySpec(getPassword(), SALT, ITERATIONS, KEY_SIZE);
            SecretKey tmp = factory.generateSecret(spec);
            secret = new SecretKeySpec(tmp.getEncoded(), "AES");

            // CBC = Cipher Block chaining
            // PKCS5Padding Indicates that the keys are padded
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        } catch (Exception e) {
            throw new RuntimeException("Unable to initialize " + this.getClass().getName(), e);
        }
    }

    @Override
    public String decrypt(String input) {
        if (!input.startsWith(CRYPT_PREFIX)) {
            throw new RuntimeException("Unable to decrypt, input string does not start with 'CRYPT'.");
        }

        try {
            String encodedValue = input.substring(6, input.length());
            byte[] data = base64Decoder.decode(encodedValue);
            int keylen = KEY_SIZE / 8;
            byte[] iv = new byte[keylen];
            System.arraycopy(data, 0, iv, 0, keylen);
            cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
            return new String(cipher.doFinal(data, keylen, data.length - keylen), STRING_ENCODING);
        } catch (Exception e) {
            throw new RuntimeException("Unable to decrypt because the decrypted password is incorrect.", e);
        }
    }

    protected char[] getPassword() {
        return "light".toCharArray();
    }
}
