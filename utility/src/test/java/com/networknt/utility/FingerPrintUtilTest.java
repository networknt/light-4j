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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class FingerPrintUtilTest {
    @Test
    public void testGetCertFingerPrintPrimary() throws Exception {
        X509Certificate cert = null;
        try (InputStream is = Config.getInstance().getInputStreamFromFile("primary.crt")){
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            cert = (X509Certificate) cf.generateCertificate(is);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String fp = FingerPrintUtil.getCertFingerPrint(cert);
        Assertions.assertEquals("41a7d6cddc203fdd53190ee52f234872c446a739d79646a18ea95d5facd5dad1", fp);
    }

    @Test
    public void testGetCertFingerPrintSecondary() throws Exception {
        X509Certificate cert = null;
        try (InputStream is = Config.getInstance().getInputStreamFromFile("secondary.crt")){
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            cert = (X509Certificate) cf.generateCertificate(is);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String fp = FingerPrintUtil.getCertFingerPrint(cert);
        Assertions.assertEquals("3f5693c41be2473f2efcd64ac842a0770b8d2decd1a87af184247c214f7b97ce", fp);
    }

    @Test
    public void testGetCertFingerPrintAlice() throws Exception {
        X509Certificate cert = null;
        try (InputStream is = Config.getInstance().getInputStreamFromFile("alice.crt")){
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            cert = (X509Certificate) cf.generateCertificate(is);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String fp = FingerPrintUtil.getCertFingerPrint(cert);
        Assertions.assertEquals("e8730cc584b1eb172d71544d8913ee4736438dbf5d3c0f5bfc757e7228a97f73", fp);
    }

    @Test
    public void testGetCertFingerPrintBob() throws Exception {
        X509Certificate cert = null;
        try (InputStream is = Config.getInstance().getInputStreamFromFile("bob.crt")){
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            cert = (X509Certificate) cf.generateCertificate(is);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String fp = FingerPrintUtil.getCertFingerPrint(cert);
        Assertions.assertEquals("1ccfc8bbffa962b6c8c8f863878d5660bc24cc0e97532154d4311ccff9a05e69", fp);
    }

    @Test
    public void testGetCertFingerPrintCa() throws Exception {
        X509Certificate cert = null;
        try (InputStream is = Config.getInstance().getInputStreamFromFile("ca.crt")){
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            cert = (X509Certificate) cf.generateCertificate(is);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String fp = FingerPrintUtil.getCertFingerPrint(cert);
        Assertions.assertEquals("dc6264315f1dab24eb4bb0d1de763fc10da04ce253a56ac24221db5bfa4eec94", fp);
    }

    @Test
    public void testGetCertFingerPrintCarol() throws Exception {
        X509Certificate cert = null;
        try (InputStream is = Config.getInstance().getInputStreamFromFile("carol.crt")){
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            cert = (X509Certificate) cf.generateCertificate(is);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String fp = FingerPrintUtil.getCertFingerPrint(cert);
        Assertions.assertEquals("6c1a0faf16898bee1eaea9195629d86dc14d8258c0436608c4c9161dbac5d65d", fp);
    }
}
