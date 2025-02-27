package com.networknt.client;

import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.StringField;


public class TlsConfig {

    public static final String VERIFY_HOSTNAME = "verifyHostname";
    public static final String LOAD_DEFAULT_TRUST_STORE = "loadDefaultTrustStore";
    public static final String LOAD_TRUST_STORE = "loadTrustStore";
    public static final String TRUST_STORE = "trustStore";
    public static final String TRUST_STORE_PASS = "trustStorePass";
    public static final String LOAD_KEY_STORE = "loadKeyStore";
    public static final String KEY_STORE = "keyStore";
    public static final String KEY_STORE_PASS = "keyStorePass";
    public static final String KEY_PASS = "keyPass";
    public static final String DEFAULT_CERT_PASSWORD = "defaultCertPassword";
    public static final String TLS_VERSION = "tlsVersion";

    @BooleanField(
            configFieldName = VERIFY_HOSTNAME,
            externalizedKeyName = VERIFY_HOSTNAME,
            externalized = true,
            defaultValue = true,
            description = "if the server is using self-signed certificate, this need to be false. " +
                    "If true, you have to use CA signed certificate or load\n" +
                    "truststore that contains the self-signed certificate."
    )
    private boolean verifyHostname;

    @BooleanField(
            configFieldName = LOAD_DEFAULT_TRUST_STORE,
            externalizedKeyName = LOAD_DEFAULT_TRUST_STORE,
            externalized = true,
            defaultValue = true,
            description = "indicate of system load default cert."
    )
    private boolean loadDefaultTrustStore;

    @BooleanField(
            configFieldName = LOAD_TRUST_STORE,
            externalizedKeyName = LOAD_TRUST_STORE,
            externalized = true,
            defaultValue = true,
            description = "trust store contains certificates that server needs. Enable if tls is used."
    )
    private boolean loadTrustStore;

    @StringField(
            configFieldName = TRUST_STORE,
            externalizedKeyName = TRUST_STORE,
            externalized = true,
            defaultValue = "client.truststore",
            description = "trust store location can be specified here or system properties " +
                    "javax.net.ssl.trustStore and password javax.net.ssl.trustStorePassword"
    )
    private String trustStore;

    @StringField(
            configFieldName = TRUST_STORE_PASS,
            externalizedKeyName = TRUST_STORE_PASS,
            externalized = true,
            defaultValue = "password",
            description = "trust store password"
    )
    private char[] trustStorePass;

    @BooleanField(
            configFieldName = LOAD_KEY_STORE,
            externalizedKeyName = LOAD_KEY_STORE,
            externalized = true,
            description = "key store contains client key and it should be loaded if two-way ssl is used."
    )
    private boolean loadKeyStore;

    @StringField(
            configFieldName = KEY_STORE,
            externalizedKeyName = KEY_STORE,
            externalized = true,
            defaultValue = "client.keystore",
            description = "key store location"
    )
    private String keyStore;

    @StringField(
            configFieldName = KEY_STORE_PASS,
            externalizedKeyName = KEY_STORE_PASS,
            externalized = true,
            defaultValue = "password",
            description = "key store password"
    )
    private char[] keyStorePass;

    @StringField(
            configFieldName = KEY_PASS,
            externalizedKeyName = KEY_PASS,
            externalized = true,
            defaultValue = "password",
            description = "private key password"
    )
    private char[] keyPass;

    @StringField(
            configFieldName = DEFAULT_CERT_PASSWORD,
            externalizedKeyName = DEFAULT_CERT_PASSWORD,
            externalized = true,
            defaultValue = "chageit",
            description = "public issued CA cert password"
    )
    private char[] defaultCertPassword;

    @StringField(
            configFieldName = TLS_VERSION,
            externalizedKeyName = TLS_VERSION,
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
