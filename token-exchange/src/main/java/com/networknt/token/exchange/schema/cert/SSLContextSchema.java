package com.networknt.token.exchange.schema.cert;

import com.networknt.config.schema.ObjectField;
import com.networknt.config.schema.StringField;

public class SSLContextSchema {

    @StringField(configFieldName = "tlsVersion")
    private String tlsVersion;

    @ObjectField(configFieldName = "truststore", ref = TrustStoreSchema.class)
    private TrustStoreSchema trustStore;

    @ObjectField(configFieldName = "keystore", ref = KeyStoreSchema.class)
    private KeyStoreSchema keyStore;


    public TrustStoreSchema getTrustStore() {
        return trustStore;
    }

    public KeyStoreSchema getKeyStore() {
        return keyStore;
    }

    public String getTlsVersion() {
        return tlsVersion;
    }

}
