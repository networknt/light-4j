package com.networknt.client.ssl;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClientX509ExtendedTrustManager implements X509TrustManager {
    private final X509TrustManager trustManager;

    public ClientX509ExtendedTrustManager(List<TrustManager> trustManagers) {
        if(trustManagers == null || trustManagers.size() == 0) {
            throw new IllegalArgumentException("TrustManagers must not be null or empty");
        }
        this.trustManager = (X509TrustManager)trustManagers.get(0);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        try {
            trustManager.checkClientTrusted(chain, authType);
        } catch (CertificateException e) {
            throw new CertificateException("None of the TrustManagers trust this certificate chain");
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        try {
            trustManager.checkServerTrusted(chain, authType);
            return; // someone trusts them. success!
        } catch (CertificateException e) {
            throw new CertificateException("None of the TrustManagers trust this certificate chain");
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        List<X509Certificate> certificates = new ArrayList<>(Arrays.asList(trustManager.getAcceptedIssuers()));
        return certificates.toArray(new X509Certificate[0]);
    }
}
