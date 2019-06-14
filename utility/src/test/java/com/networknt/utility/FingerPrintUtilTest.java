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
import org.junit.Assert;
import org.junit.Test;

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
        Assert.assertEquals("564aa231f84039ce2b2b886e58f88dcee26fa3e3", fp);
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
        Assert.assertEquals("0775dcf9193095e791307a115c192cc897753499", fp);
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
        Assert.assertEquals("0ea49f0d1f89ae839e96c3665beb4ff6d0033c33", fp);
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
        Assert.assertEquals("921b97842f23474d8961bfd54911c298316aa558", fp);
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
        Assert.assertEquals("da2794f442f08a73ac9eef7f9378dd7a5bbcf8c6", fp);
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
        Assert.assertEquals("f9d76aae4799610a3c904df073dc79f430b408b1", fp);
    }
}
