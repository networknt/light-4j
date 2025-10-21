package com.networknt.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.MapField;
import com.networknt.config.schema.StringField;

import java.util.Map;


public class TlsConfig {



    @BooleanField(
            configFieldName = ClientConfig.VERIFY_HOSTNAME,
            externalizedKeyName = ClientConfig.VERIFY_HOSTNAME,
            externalized = true,
            defaultValue = "true",
            description = "if the server is using self-signed certificate, this need to be false. " +
                    "If true, you have to use CA signed certificate or load\n" +
                    "truststore that contains the self-signed certificate."
    )
    @JsonProperty(ClientConfig.VERIFY_HOSTNAME)
    private Boolean verifyHostname = true;

    @BooleanField(
            configFieldName = ClientConfig.LOAD_DEFAULT_TRUST_STORE,
            externalizedKeyName = ClientConfig.LOAD_DEFAULT_TRUST_STORE,
            externalized = true,
            defaultValue = "true",
            description = "indicate of system load default cert."
    )
    @JsonProperty(ClientConfig.LOAD_DEFAULT_TRUST_STORE)
    private Boolean loadDefaultTrustStore = true;

    @BooleanField(
            configFieldName = ClientConfig.LOAD_TRUST_STORE,
            externalizedKeyName = ClientConfig.LOAD_TRUST_STORE,
            externalized = true,
            defaultValue = "true",
            description = "trust store contains certificates that server needs. Enable if tls is used."
    )
    @JsonProperty(ClientConfig.LOAD_TRUST_STORE)
    private Boolean loadTrustStore = true;

    @StringField(
            configFieldName = ClientConfig.TRUST_STORE,
            externalizedKeyName = ClientConfig.TRUST_STORE,
            externalized = true,
            defaultValue = "client.truststore",
            description = "trust store location can be specified here or system properties " +
                    "javax.net.ssl.trustStore and password javax.net.ssl.trustStorePassword"
    )
    @JsonProperty(ClientConfig.TRUST_STORE)
    private String trustStore = "client.truststore";

    @StringField(
            configFieldName = ClientConfig.TRUST_STORE_PASS,
            externalizedKeyName = ClientConfig.TRUST_STORE_PASS,
            externalized = true,
            defaultValue = "password",
            description = "trust store password"
    )
    @JsonProperty(ClientConfig.TRUST_STORE_PASS)
    private char[] trustStorePass = "password".toCharArray();

    @BooleanField(
            configFieldName = ClientConfig.LOAD_KEY_STORE,
            externalizedKeyName = ClientConfig.LOAD_KEY_STORE,
            externalized = true,
            description = "key store contains client key and it should be loaded if two-way ssl is used.",
            defaultValue = "false"
    )
    @JsonProperty(ClientConfig.LOAD_KEY_STORE)
    private Boolean loadKeyStore;

    @StringField(
            configFieldName = ClientConfig.KEY_STORE,
            externalizedKeyName = ClientConfig.KEY_STORE,
            externalized = true,
            defaultValue = "client.keystore",
            description = "key store location"
    )
    @JsonProperty(ClientConfig.KEY_STORE)
    private String keyStore = "client.keystore";

    @StringField(
            configFieldName = ClientConfig.KEY_STORE_PASS,
            externalizedKeyName = ClientConfig.KEY_STORE_PASS,
            externalized = true,
            defaultValue = "password",
            description = "key store password"
    )
    @JsonProperty(ClientConfig.KEY_STORE_PASS)
    private char[] keyStorePass = "password".toCharArray();

    @StringField(
            configFieldName = ClientConfig.KEY_PASS,
            externalizedKeyName = ClientConfig.KEY_PASS,
            externalized = true,
            defaultValue = "password",
            description = "private key password"
    )
    @JsonProperty(ClientConfig.KEY_PASS)
    private char[] keyPass = "password".toCharArray();

    @StringField(
            configFieldName = ClientConfig.DEFAULT_CERT_PASSWORD,
            externalizedKeyName = ClientConfig.DEFAULT_CERT_PASSWORD,
            externalized = true,
            defaultValue = "changeit",
            description = "public issued CA cert password"
    )
    @JsonProperty(ClientConfig.DEFAULT_CERT_PASSWORD)
    private char[] defaultCertPassword = "changeit".toCharArray();

    @StringField(
            configFieldName = ClientConfig.TLS_VERSION,
            externalizedKeyName = ClientConfig.TLS_VERSION,
            externalized = true,
            defaultValue = "TLSv1.3",
            description = "TLS version. Default is TSLv1.3, and you can downgrade to TLSv1.2 to support " +
                    "some internal old servers that support only TLSv1.1\n" +
                    "and 1.2 (deprecated and risky)."
    )
    @JsonProperty(ClientConfig.TLS_VERSION)
    private String tlsVersion = "TLSv1.3";

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
