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
