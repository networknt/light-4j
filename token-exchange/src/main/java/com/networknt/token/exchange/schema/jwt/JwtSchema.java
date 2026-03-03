package com.networknt.token.exchange.schema.jwt;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.networknt.config.schema.IntegerField;
import com.networknt.config.schema.ObjectField;
import com.networknt.config.schema.StringField;
import com.networknt.token.exchange.schema.TtlUnit;
import com.networknt.token.exchange.schema.cert.KeyStoreSchema;

public class JwtSchema {

    @StringField(configFieldName = "algorithm")
    @JsonProperty("algorithm")
    private String algorithm;

    @IntegerField(configFieldName = "jwtTtl")
    @JsonProperty("jwtTtl")
    private long jwtTtl;

    @StringField(configFieldName = "ttlUnit")
    @JsonProperty("ttlUnit")
    @JsonSetter(nulls = Nulls.SKIP)
    private TtlUnit ttlUnit = TtlUnit.SECOND;

    @ObjectField(configFieldName = "keyStore", ref = KeyStoreSchema.class)
    @JsonProperty("keyStore")
    private KeyStoreSchema keyStore;

    @ObjectField(configFieldName = "jwtHeader", ref = JwtPartialSchema.class)
    @JsonProperty("jwtHeader")
    private JwtPartialSchema jwtHeader;

    @ObjectField(configFieldName = "jwtBody", ref = JwtPartialSchema.class)
    @JsonProperty("jwtBody")
    private JwtPartialSchema jwtBody;

    public long getJwtTtl() {
        return jwtTtl;
    }

    public TtlUnit getTtlUnit() {
        return ttlUnit;
    }

    public KeyStoreSchema getKeyStore() {
        return keyStore;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public JwtPartialSchema getJwtHeader() {
        return jwtHeader;
    }

    public JwtPartialSchema getJwtBody() {
        return jwtBody;
    }
}
