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
