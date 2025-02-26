package com.networknt.client;

import com.networknt.config.Config;
import com.networknt.config.MapLoadable;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.IntegerField;
import com.networknt.config.schema.ObjectField;
import com.networknt.config.schema.StringField;

import java.util.Map;

public class OAuthTokenConfig /*implements MapLoadable*/ {

    public static final String CACHE = "cache";
    public static final String TOKEN_RENEW_BEFORE_EXPIRED = "tokenRenewBeforeExpired";
    public static final String EXPIRED_REFRESH_RETRY_DELAY = "expiredRefreshRetryDelay";
    public static final String EARLY_REFRESH_RETRY_DELAY = "earlyRefreshRetryDelay";
    public static final String SERVER_URL = "server_url";
    public static final String SERVICE_ID = "serviceId";
    public static final String PROXY_HOST = "proxyHost";
    public static final String PROXY_PORT = "proxyPort";
    public static final String ENABLE_HTTP_2 = "enableHttp2";
    public static final String AUTHORIZATION_CODE = "authorization_code";
    public static final String CLIENT_CREDENTIALS = "client_credentials";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String KEY = "key";
    @ObjectField(
            configFieldName = CACHE,
            useSubObjectDefault = true,
            ref = OauthTokenCacheConfig.class
    )
    private OauthTokenCacheConfig cache;

    @IntegerField(
            configFieldName = TOKEN_RENEW_BEFORE_EXPIRED,
            externalizedKeyName = TOKEN_RENEW_BEFORE_EXPIRED,
            externalized = true,
            defaultValue = 60000,
            description = "The scope token will be renewed automatically 1 minute before expiry"
    )
    private int tokenRenewBeforeExpired;

    @IntegerField(
            configFieldName = EXPIRED_REFRESH_RETRY_DELAY,
            externalizedKeyName = EXPIRED_REFRESH_RETRY_DELAY,
            externalized = true,
            defaultValue = 2000,
            description = "if scope token is expired, we need short delay so that we can retry faster."
    )
    private int expiredRefreshRetryDelay;

    @IntegerField(
            configFieldName = EARLY_REFRESH_RETRY_DELAY,
            externalizedKeyName = EARLY_REFRESH_RETRY_DELAY,
            externalized = true,
            defaultValue = 4000,
            description = "if scope token is not expired but in renew window, we need slow retry delay."
    )
    private int earlyRefreshRetryDelay;

    @StringField(
            configFieldName = SERVER_URL,
            externalizedKeyName = "tokenServerUrl",
            externalized = true,
            description = "token server url. The default port number for token service is 6882. If this is set,\n" +
                    "it will take high priority than serviceId for the direct connection"
    )
    private String server_url;

    @StringField(
            configFieldName = SERVICE_ID,
            externalizedKeyName = "tokenServiceId",
            externalized = true,
            defaultValue = "com.networknt.oauth2-token-1.0.0",
            description = "token service unique id for OAuth 2.0 provider. If server_url is not set above,\n" +
                    "a service discovery action will be taken to find an instance of token service."
    )
    private String serviceId;

    @StringField(
            configFieldName = PROXY_HOST,
            externalizedKeyName = "tokenProxyHost",
            externalized = true,
            description = "For users who leverage SaaS OAuth 2.0 provider from lightapi.net or others in the public cloud\n" +
                    "and has an internal proxy server to access code, token and key services of OAuth 2.0, set up the\n" +
                    "proxyHost here for the HTTPS traffic. This option is only working with server_url and serviceId\n" +
                    "below should be commented out. OAuth 2.0 services cannot be discovered if a proxy server is used."
    )
    private String proxyHost;

    @IntegerField(
            configFieldName = PROXY_PORT,
            externalizedKeyName = "tokenProxyPort",
            externalized = true,
            description = "We only support HTTPS traffic for the proxy and the default port is 443. If your proxy server has\n" +
                    "a different port, please specify it here. If proxyHost is available and proxyPort is missing, then\n" +
                    "the default value 443 is going to be used for the HTTP connection."
    )
    private int proxyPort;

    @BooleanField(
            configFieldName = ENABLE_HTTP_2,
            externalizedKeyName = "tokenEnableHttp2",
            defaultValue = true,
            externalized = true,
            description = "set to true if the oauth2 provider supports HTTP/2"
    )
    private boolean enableHttp2;

    @ObjectField(
            configFieldName = AUTHORIZATION_CODE,
            useSubObjectDefault = true,
            ref = OAuthTokenAuthorizationCodeConfig.class,
            description = "the following section defines uri and parameters for authorization code grant type"
    )
    private OAuthTokenAuthorizationCodeConfig authorization_code;

    @ObjectField(
            configFieldName = CLIENT_CREDENTIALS,
            useSubObjectDefault = true,
            ref = OAuthTokenClientCredentialConfig.class,
            description = "the following section defines uri and parameters for client credentials grant type"
    )
    private OAuthTokenClientCredentialConfig client_credentials;

    @ObjectField(
            configFieldName = REFRESH_TOKEN,
            useSubObjectDefault = true,
            ref = OAuthTokenRefreshTokenConfig.class
    )
    private OAuthTokenRefreshTokenConfig refresh_token;

    @ObjectField(
            configFieldName = KEY,
            useSubObjectDefault = true,
            ref = OAuthTokenKeyConfig.class,
            description = "light-oauth2 key distribution endpoint configuration for token verification"
    )
    private OAuthTokenKeyConfig key;

    public OauthTokenCacheConfig getCache() {
        return cache;
    }

    public int getTokenRenewBeforeExpired() {
        return tokenRenewBeforeExpired;
    }

    public int getExpiredRefreshRetryDelay() {
        return expiredRefreshRetryDelay;
    }

    public int getEarlyRefreshRetryDelay() {
        return earlyRefreshRetryDelay;
    }

    public String getServer_url() {
        return server_url;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public boolean isEnableHttp2() {
        return enableHttp2;
    }

    public OAuthTokenAuthorizationCodeConfig getAuthorization_code() {
        return authorization_code;
    }

    public OAuthTokenClientCredentialConfig getClient_credentials() {
        return client_credentials;
    }

    public OAuthTokenRefreshTokenConfig getRefresh_token() {
        return refresh_token;
    }

    public OAuthTokenKeyConfig getKey() {
        return key;
    }

//    @Override
//    public void loadData(Map<String, Object> data) {
//        if (data.containsKey(CACHE)) {
//            cache.loadData((Map<String, Object>) data.get(CACHE));
//        }
//
//        if (data.containsKey(TOKEN_RENEW_BEFORE_EXPIRED)) {
//            tokenRenewBeforeExpired = Config.loadIntegerValue(TOKEN_RENEW_BEFORE_EXPIRED, data.get(TOKEN_RENEW_BEFORE_EXPIRED));
//        }
//
//        if (data.containsKey(EXPIRED_REFRESH_RETRY_DELAY)) {
//            expiredRefreshRetryDelay = Config.loadIntegerValue(EXPIRED_REFRESH_RETRY_DELAY, data.get(EXPIRED_REFRESH_RETRY_DELAY));
//        }
//
//        if (data.containsKey(EARLY_REFRESH_RETRY_DELAY)) {
//            earlyRefreshRetryDelay = Config.loadIntegerValue(EARLY_REFRESH_RETRY_DELAY, data.get(EARLY_REFRESH_RETRY_DELAY));
//        }
//
//        if (data.containsKey(SERVER_URL)) {
//            server_url = (String) data.get(SERVER_URL);
//        }
//
//        if (data.containsKey(SERVICE_ID)) {
//            serviceId = (String) data.get(SERVICE_ID);
//        }
//
//        if (data.containsKey(PROXY_HOST)) {
//            proxyHost = (String) data.get(PROXY_HOST);
//        }
//
//        if (data.containsKey(PROXY_PORT)) {
//            proxyPort = Config.loadIntegerValue(PROXY_PORT, data.get(PROXY_PORT));
//        }
//
//        if (data.containsKey(ENABLE_HTTP_2)) {
//            enableHttp2 = Config.loadBooleanValue(ENABLE_HTTP_2, data.get(ENABLE_HTTP_2));
//        }
//
//        if (data.containsKey(AUTHORIZATION_CODE)) {
//            authorization_code.loadData((Map<String, Object>) data.get(AUTHORIZATION_CODE));
//        }
//
//        if (data.containsKey(CLIENT_CREDENTIALS)) {
//            client_credentials.loadData((Map<String, Object>) data.get(CLIENT_CREDENTIALS));
//        }
//
//        if (data.containsKey(REFRESH_TOKEN)) {
//            refresh_token.loadData((Map<String, Object>) data.get(REFRESH_TOKEN));
//        }
//
//        if (data.containsKey(KEY)) {
//            key.loadData((Map<String, Object>) data.get(KEY));
//        }
//    }
}
