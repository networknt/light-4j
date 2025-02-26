package com.networknt.client;

import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.StringField;


public class TlsConfig {

    @BooleanField(
            configFieldName = "verifyHostname",
            externalizedKeyName = "verifyHostname",
            externalized = true,
            defaultValue = true,
            description = "if the server is using self-signed certificate, this need to be false. " +
                    "If true, you have to use CA signed certificate or load\n" +
                    "truststore that contains the self-signed certificate."
    )
    private boolean verifyHostname;

    @BooleanField(
            configFieldName = "loadDefaultTrustStore",
            externalizedKeyName = "loadDefaultTrustStore",
            externalized = true,
            defaultValue = true,
            description = "indicate of system load default cert."
    )
    private boolean loadDefaultTrustStore;

    @BooleanField(
            configFieldName = "loadTrustStore",
            externalizedKeyName = "loadTrustStore",
            externalized = true,
            defaultValue = true,
            description = "trust store contains certificates that server needs. Enable if tls is used."
    )
    private boolean loadTrustStore;

    @StringField(
            configFieldName = "trustStore",
            externalizedKeyName = "trustStore",
            externalized = true,
            defaultValue = "client.truststore",
            description = "trust store location can be specified here or system properties " +
                    "javax.net.ssl.trustStore and password javax.net.ssl.trustStorePassword"
    )
    private String trustStore;

    @StringField(
            configFieldName = "trustStorePass",
            externalizedKeyName = "trustStorePass",
            externalized = true,
            defaultValue = "password",
            description = "trust store password"
    )
    private char[] trustStorePass;

    @BooleanField(
            configFieldName = "loadKeyStore",
            externalizedKeyName = "loadKeyStore",
            externalized = true,
            description = "key store contains client key and it should be loaded if two-way ssl is used."
    )
    private boolean loadKeyStore;

    @StringField(
            configFieldName = "keyStore",
            externalizedKeyName = "keyStore",
            externalized = true,
            defaultValue = "client.keystore",
            description = "key store location"
    )
    private String keyStore;

    @StringField(
            configFieldName = "keyStorePass",
            externalizedKeyName = "keyStorePass",
            externalized = true,
            defaultValue = "password",
            description = "key store password"
    )
    private char[] keyStorePass;

    @StringField(
            configFieldName = "keyPass",
            externalizedKeyName = "keyPass",
            externalized = true,
            defaultValue = "password",
            description = "private key password"
    )
    private char[] keyPass;

    @StringField(
            configFieldName = "defaultCertPassword",
            externalizedKeyName = "defaultCertPassword",
            externalized = true,
            defaultValue = "chageit",
            description = "public issued CA cert password"
    )
    private char[] defaultCertPassword;

    @StringField(
            configFieldName = "tlsVersion",
            externalizedKeyName = "tlsVersion",
            externalized = true,
            defaultValue = "TLSv1.3",
            description = "TLS version. Default is TSLv1.3, and you can downgrade to TLSv1.2 to support " +
                    "some internal old servers that support only TLSv1.1\n" +
                    "and 1.2 (deprecated and risky)."
    )
    String tlsVersion;

    public boolean isVerifyHostname() {
        return verifyHostname;
    }

    public boolean isLoadDefaultTrustStore() {
        return loadDefaultTrustStore;
    }

    public String getTrustStore() {
        return trustStore;
    }

    public char[] getTrustStorePass() {
        return trustStorePass;
    }

    public boolean isLoadTrustStore() {
        return loadTrustStore;
    }

    public boolean isLoadKeyStore() {
        return loadKeyStore;
    }

    public char[] getDefaultCertPassword() {
        return defaultCertPassword;
    }

    public String getKeyStore() {
        return keyStore;
    }

    public char[] getKeyStorePass() {
        return keyStorePass;
    }

    public char[] getKeyPass() {
        return keyPass;
    }

    public String getTlsVersion() {
        return tlsVersion;
    }
}
