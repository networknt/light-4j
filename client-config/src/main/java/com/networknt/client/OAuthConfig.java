package com.networknt.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.ObjectField;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OAuthConfig {

    @BooleanField(
            configFieldName = ClientConfig.MULTIPLE_AUTH_SERVERS,
            externalizedKeyName = ClientConfig.MULTIPLE_AUTH_SERVERS,
            description = "OAuth 2.0 token endpoint configuration\n" +
                    "If there are multiple oauth providers per serviceId, then we need to update this flag to true. " +
                    "In order to derive the serviceId from the\n" +
                    "path prefix, we need to set up the pathPrefixServices " +
                    "below if there is no duplicated paths between services.",
            defaultValue = "false"
    )
    @JsonProperty(ClientConfig.MULTIPLE_AUTH_SERVERS)
    private Boolean multipleAuthServers = false;

    @ObjectField(
            configFieldName = ClientConfig.TOKEN,
            useSubObjectDefault = true,
            ref = OAuthTokenConfig.class
    )
    @JsonProperty(ClientConfig.TOKEN)
    private OAuthTokenConfig token = null;

    @ObjectField(
            configFieldName = ClientConfig.SIGN,
            useSubObjectDefault = true,
            ref = OAuthSignConfig.class,
            description = "Sign endpoint configuration"
    )
    @JsonProperty(ClientConfig.SIGN)
    private OAuthSignConfig sign = null;

    @ObjectField(
            configFieldName = ClientConfig.DEREF,
            useSubObjectDefault = true,
            ref = OAuthDerefConfig.class,
            description = "de-ref by reference token to JWT token. " +
                    "It is separate service as it might be the external OAuth 2.0 provider."
    )
    @JsonProperty(ClientConfig.DEREF)
    private OAuthDerefConfig deref = null;

    public boolean isMultipleAuthServers() {
        return multipleAuthServers;
    }

    public OAuthTokenConfig getToken() {
        return token;
    }

    public OAuthDerefConfig getDeref() {
        return deref;
    }

    public OAuthSignConfig getSign() {
        return sign;
    }
}
