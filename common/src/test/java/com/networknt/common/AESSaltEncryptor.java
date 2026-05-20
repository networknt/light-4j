package com.networknt.common;

import com.networknt.utility.Constants;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import static com.networknt.decrypt.Decryptor.CRYPT_PREFIX;
import static java.lang.System.exit;

/**
 * This is a new encryptor utility to encrypt the secret with stronger implementation.
 * Instead of using a static salt, we are generating a salt for each invocation to make
 * dictionary attack harder.
 */
public class AESSaltEncryptor {
    public static void main(String [] args) {
        if(args.length == 0) {
            System.out.println("Please provide plain text to encrypt!");
            exit(0);
        }
        AESSaltEncryptor encryptor = new AESSaltEncryptor();
        System.out.println(encryptor.encrypt(args[0]));
    }

    private static final int ITERATIONS = 65536;
    private static final int KEY_SIZE = 256;
    private static final String STRING_ENCODING = "UTF-8";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static byte[] salt;
    private SecretKeySpec secret;
    private Cipher cipher;

    public AESSaltEncryptor() {
        try {
            /* Derive the key, given password and salt. */
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            salt = getSalt();
            KeySpec spec = new PBEKeySpec(Constants.FRAMEWORK_NAME.toCharArray(), salt, ITERATIONS, KEY_SIZE);
            SecretKey tmp = factory.generateSecret(spec);
            secret = new SecretKeySpec(tmp.getEncoded(), "AES");
            cipher = Cipher.getInstance("AES/GCM/NoPadding");
        } catch (Exception e) {
            throw new RuntimeException("Unable to initialize", e);
        }
    }

    /**
     * Encrypt given input string
     *
     * @param input
     * @return
     * @throws RuntimeException
     */
    public String encrypt(String input)
    {
        try
        {
            byte[] inputBytes = input.getBytes(STRING_ENCODING);
            byte[] iv = getIv();
            cipher.init(Cipher.ENCRYPT_MODE, secret, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] out = cipher.doFinal(inputBytes);
            return CRYPT_PREFIX + ":" + toHex(salt) + ":" + toHex(iv) + ":" + toHex(out);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException("Unable to encrypt", e);
        } catch (BadPaddingException e) {
            throw new RuntimeException("Unable to encrypt", e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Unable to encrypt", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unable to encrypt", e);
        } catch(NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to encrypt", e);
        } catch(InvalidAlgorithmParameterException e) {
            throw new RuntimeException("Unable to encrypt", e);
        }
    }

    private static byte[] getSalt() throws NoSuchAlgorithmException
    {
        byte[] salt = new byte[16];
        SECURE_RANDOM.nextBytes(salt);
        return salt;
    }

    private static byte[] getIv() throws NoSuchAlgorithmException
    {
        byte[] iv = new byte[GCM_IV_LENGTH];
        SECURE_RANDOM.nextBytes(iv);
        return iv;
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
}
