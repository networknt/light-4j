package com.networknt.token.exchange.schema.cert;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.networknt.config.schema.StringField;

public class KeyStoreSchema {

    @StringField(configFieldName = "name")
    @JsonProperty("name")
    private String name;

    @StringField(configFieldName = "password")
    @JsonProperty("password")
    private char[] password;

    @StringField(configFieldName = "alias")
    @JsonProperty("alias")
    private String alias;

    @StringField(configFieldName = "keyPass")
    @JsonProperty("keyPass")
    private char[] keyPass;

    @StringField(configFieldName = "algorithm")
    @JsonProperty("algorithm")
    private String algorithm;

    public String getName() {
        return name;
    }

    public char[] getPassword() {
        return password;
    }

    public String getAlias() {
        return alias;
    }

    public char[] getKeyPass() {
        return keyPass;
    }

    public String getAlgorithm() {
        return algorithm;
    }
}
