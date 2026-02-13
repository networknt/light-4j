package com.networknt.token.exchange.schema.cert;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.networknt.config.schema.StringField;

public class TrustStoreSchema {

    @StringField(configFieldName = "algorithm")
    @JsonProperty("algorithm")
    private String algorithm;

    @StringField(configFieldName = "name")
    @JsonProperty("name")
    private String name;

    @StringField(configFieldName = "password")
    @JsonProperty("password")
    private char[] password;


    public String getName() {
        return name;
    }

    public char[] getPassword() {
        return password;
    }

    public String getAlgorithm() {
        return algorithm;
    }
}
