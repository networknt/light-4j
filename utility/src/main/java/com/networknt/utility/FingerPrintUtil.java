package com.networknt.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.cert.Certificate;

import static sun.security.pkcs11.wrapper.Functions.toHexString;

public class FingerPrintUtil {
    static final Logger logger = LoggerFactory.getLogger(CodeVerifierUtil.class);

    public static String getCertFingerPrint(Certificate cert)  {
        byte [] digest = null;
        try {
            byte[] encCertInfo = cert.getEncoded();
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            digest = md.digest(encCertInfo);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return toHexString(digest);
    }
}
