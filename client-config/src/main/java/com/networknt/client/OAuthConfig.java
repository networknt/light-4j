package com.networknt.client;

import com.networknt.config.Config;
import com.networknt.config.MapLoadable;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.ObjectField;

import java.util.Map;

public class OAuthConfig /*implements MapLoadable*/ {

    public static final String TOKEN = "token";
    public static final String MULTIPLE_AUTH_SERVERS = "multipleAuthServers";
    public static final String DEREF = "deref";
    public static final String SIGN = "sign";

    @BooleanField(
            configFieldName = MULTIPLE_AUTH_SERVERS,
            externalizedKeyName = MULTIPLE_AUTH_SERVERS,
            externalized = true,
            description = "OAuth 2.0 token endpoint configuration\n" +
                    "If there are multiple oauth providers per serviceId, then we need to update this flag to true. " +
                    "In order to derive the serviceId from the\n" +
                    "path prefix, we need to set up the pathPrefixServices " +
                    "below if there is no duplicated paths between services."
    )
    private boolean multipleAuthServers;

    @ObjectField(
            configFieldName = TOKEN,
            useSubObjectDefault = true,
            ref = OAuthTokenConfig.class
    )
    private OAuthTokenConfig token;

    @ObjectField(
            configFieldName = SIGN,
            useSubObjectDefault = true,
            ref = OAuthSignKeyConfig.class,
            description = "Sign endpoint configuration"
    )
    private OauthSignConfig sign;

    @ObjectField(
            configFieldName = DEREF,
            useSubObjectDefault = true,
            ref = OAuthDerefConfig.class,
            description = "de-ref by reference token to JWT token. " +
                    "It is separate service as it might be the external OAuth 2.0 provider."
    )
    private OAuthDerefConfig deref;

    public boolean isMultipleAuthServers() {
        return multipleAuthServers;
    }

    public OAuthTokenConfig getToken() {
        return token;
    }

    public OAuthDerefConfig getDeref() {
        return deref;
    }

    public OauthSignConfig getSign() {
        return sign;
    }

//    @Override
//    public void loadData(Map<String, Object> data) {
//        if (data.get(MULTIPLE_AUTH_SERVERS) != null) {
//            this.multipleAuthServers = Config.loadBooleanValue(MULTIPLE_AUTH_SERVERS, data.get(MULTIPLE_AUTH_SERVERS));
//        }
//
//        if (data.get(TOKEN) instanceof Map) {
//            this.token.loadData((Map<String, Object>) data.get(TOKEN));
//        }
//
//        if (data.get(SIGN) instanceof Map) {
//            this.sign.loadData((Map<String, Object>) data.get(SIGN));
//        }
//
//        if (data.get(DEREF) instanceof Map) {
//            this.deref.loadData((Map<String, Object>) data.get(DEREF));
//        }
//    }
}
