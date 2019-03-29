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

import com.networknt.config.Config;
import org.junit.Test;

import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class FingerPrintUtilTest {
    @Test
    public void testGetCertFingerPrint() throws Exception {
        X509Certificate cert = null;
        try (InputStream is = Config.getInstance().getInputStreamFromFile("primary.crt")){
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            cert = (X509Certificate) cf.generateCertificate(is);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String fp = FingerPrintUtil.getCertFingerPrint(cert);
        System.out.println("fingerprint = " + fp);
    }

}
