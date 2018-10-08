package com.networknt.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.cert.Certificate;

public class FingerPrintUtil {
    static final Logger logger = LoggerFactory.getLogger(CodeVerifierUtil.class);
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String getCertFingerPrint(Certificate cert)  {
        byte [] digest = null;
        try {
            byte[] encCertInfo = cert.getEncoded();
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            digest = md.digest(encCertInfo);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (digest != null) {
            return bytesToHex(digest).toLowerCase();
        }
        return null;
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
