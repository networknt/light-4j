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
