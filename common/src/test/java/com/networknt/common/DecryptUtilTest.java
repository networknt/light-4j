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

package com.networknt.common;

import com.networknt.config.Config;
import com.networknt.decrypt.AESSaltDecryptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Map;

public class DecryptUtilTest {
    private static final String SECRET_KEY = "my_super_secret_key";
    private static final String SALT = "ssshhhhhhhhhhh!!!!";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    @Test
    public void testDecryptMap() {
        Map<String, Object> secretMap = Config.getInstance().getJsonMapConfig("secret");
        DecryptUtil.decryptMap(secretMap);
        Assertions.assertEquals("password", secretMap.get("serverKeystorePass"));
    }

    @Test
    public void testAESSaltEncryptor() {
        String p = "password";
        AESSaltEncryptor encryptor = new AESSaltEncryptor();
        String s = encryptor.encrypt(p);
        System.out.println("s = " + s);
        AESSaltDecryptor decryptor = new AESSaltDecryptor();
        String c = decryptor.decrypt(s);
        System.out.println("c = " + c);
    }

    @Test
    public void testAESSaltEncryptorWithRealPass() {
        String p = "tN-(kw^tQ\\46}Bq";
        AESSaltEncryptor encryptor = new AESSaltEncryptor();
        String s = encryptor.encrypt(p);
        System.out.println("s = " + s);
        AESSaltDecryptor decryptor = new AESSaltDecryptor();
        String c = decryptor.decrypt(s);
        System.out.println("c = " + c);
    }

    @Test
    public void test256() {
        String originalString = "howtodoinjava.com";

        String encryptedString = encrypt(originalString);
        String decryptedString = decrypt(encryptedString);

        System.out.println(originalString);
        System.out.println(encryptedString);
        System.out.println(decryptedString);
    }

    public static String encrypt(String strToEncrypt) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(SECRET_KEY.toCharArray(), SALT.getBytes(), 65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8));
            byte[] output = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, output, 0, iv.length);
            System.arraycopy(ciphertext, 0, output, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(output);
        } catch (Exception e) {
            System.out.println("Error while encrypting: " + e.toString());
        }
        return null;
    }

    public static String decrypt(String strToDecrypt) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(SECRET_KEY.toCharArray(), SALT.getBytes(), 65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

            byte[] input = Base64.getDecoder().decode(strToDecrypt);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[input.length - GCM_IV_LENGTH];
            System.arraycopy(input, 0, iv, 0, iv.length);
            System.arraycopy(input, iv.length, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.out.println("Error while decrypting: " + e.toString());
        }
        return null;
    }
}
