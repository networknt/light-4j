package com.networknt.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.IntegerField;
import com.networknt.config.schema.ObjectField;
import com.networknt.config.schema.StringField;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OAuthTokenConfig  {

    @ObjectField(
            configFieldName = ClientConfig.CACHE,
            useSubObjectDefault = true,
            ref = OauthTokenCacheConfig.class
    )
    @JsonProperty(ClientConfig.CACHE)
    private OauthTokenCacheConfig cache = null;

    @IntegerField(
            configFieldName = ClientConfig.TOKEN_RENEW_BEFORE_EXPIRED,
            externalizedKeyName = ClientConfig.TOKEN_RENEW_BEFORE_EXPIRED,
            externalized = true,
            defaultValue = "60000",
            description = "The scope token will be renewed automatically 1 minute before expiry"
    )
    @JsonProperty(ClientConfig.TOKEN_RENEW_BEFORE_EXPIRED)
    private Integer tokenRenewBeforeExpired = 60000;

    @IntegerField(
            configFieldName = ClientConfig.EXPIRED_REFRESH_RETRY_DELAY,
            externalizedKeyName = ClientConfig.EXPIRED_REFRESH_RETRY_DELAY,
            externalized = true,
            defaultValue = "2000",
            description = "if scope token is expired, we need short delay so that we can retry faster."
    )
    @JsonProperty(ClientConfig.EXPIRED_REFRESH_RETRY_DELAY)
    private Integer expiredRefreshRetryDelay = 2000;

    @IntegerField(
            configFieldName = ClientConfig.EARLY_REFRESH_RETRY_DELAY,
            externalizedKeyName = ClientConfig.EARLY_REFRESH_RETRY_DELAY,
            externalized = true,
            defaultValue = "4000",
            description = "if scope token is not expired but in renew window, we need slow retry delay."
    )
    @JsonProperty(ClientConfig.EARLY_REFRESH_RETRY_DELAY)
    private Integer earlyRefreshRetryDelay = 4000;

    @StringField(
            configFieldName = ClientConfig.SERVER_URL,
            externalizedKeyName = "tokenServerUrl",
            externalized = true,
            description = "token server url. The default port number for token service is 6882. If this is set,\n" +
                    "it will take high priority than serviceId for the direct connection"
    )
    @JsonProperty(ClientConfig.SERVER_URL)
    private String serverUrl = null;

    @StringField(
            configFieldName = ClientConfig.SERVICE_ID,
            externalizedKeyName = "tokenServiceId",
            externalized = true,
            defaultValue = "com.networknt.oauth2-token-1.0.0",
            description = "token service unique id for OAuth 2.0 provider. If server_url is not set above,\n" +
                    "a service discovery action will be taken to find an instance of token service."
    )
    @JsonProperty(ClientConfig.SERVICE_ID)
    private String serviceId = "com.networknt.oauth2-token-1.0.0";

    @StringField(
            configFieldName = ClientConfig.PROXY_HOST,
            externalizedKeyName = "tokenProxyHost",
            externalized = true,
            description = "For users who leverage SaaS OAuth 2.0 provider from lightapi.net or others in the public cloud\n" +
                    "and has an internal proxy server to access code, token and key services of OAuth 2.0, set up the\n" +
                    "proxyHost here for the HTTPS traffic. This option is only working with server_url and serviceId\n" +
                    "below should be commented out. OAuth 2.0 services cannot be discovered if a proxy server is used."
    )
    @JsonProperty(ClientConfig.PROXY_HOST)
    private String proxyHost = null;

    @IntegerField(
            configFieldName = ClientConfig.PROXY_PORT,
            externalizedKeyName = "tokenProxyPort",
            min = 0,
            max = 65535,
            externalized = true,
            description = "We only support HTTPS traffic for the proxy and the default port is 443. If your proxy server has\n" +
                    "a different port, please specify it here. If proxyHost is available and proxyPort is missing, then\n" +
                    "the default value 443 is going to be used for the HTTP connection."
    )
    @JsonProperty(ClientConfig.PROXY_PORT)
    private Integer proxyPort = null;

    @BooleanField(
            configFieldName = ClientConfig.ENABLE_HTTP_2,
            externalizedKeyName = "tokenEnableHttp2",
            defaultValue = "true",
            externalized = true,
            description = "set to true if the oauth2 provider supports HTTP/2"
    )
    @JsonProperty(ClientConfig.ENABLE_HTTP_2)
    private Boolean enableHttp2 = true;

    @ObjectField(
            configFieldName = ClientConfig.AUTHORIZATION_CODE,
            useSubObjectDefault = true,
            ref = OAuthTokenAuthorizationCodeConfig.class,
            description = "the following section defines uri and parameters for authorization code grant type"
    )
    @JsonProperty(ClientConfig.AUTHORIZATION_CODE)
    private OAuthTokenAuthorizationCodeConfig authorizationCode = null;

    @ObjectField(
            configFieldName = ClientConfig.CLIENT_CREDENTIALS,
            useSubObjectDefault = true,
            ref = OAuthTokenClientCredentialConfig.class,
            description = "the following section defines uri and parameters for client credentials grant type"
    )
    @JsonProperty(ClientConfig.CLIENT_CREDENTIALS)
    private OAuthTokenClientCredentialConfig clientCredentials = null;

    @ObjectField(
            configFieldName = ClientConfig.REFRESH_TOKEN,
            useSubObjectDefault = true,
            ref = OAuthTokenRefreshTokenConfig.class
    )
    @JsonProperty(ClientConfig.REFRESH_TOKEN)
    private OAuthTokenRefreshTokenConfig refreshToken = null;

    @ObjectField(
            configFieldName = ClientConfig.TOKEN_EXCHANGE,
            useSubObjectDefault = true,
            ref = OAuthTokenExchangeConfig.class
    )
    @JsonProperty(ClientConfig.TOKEN_EXCHANGE)
    private OAuthTokenExchangeConfig tokenExchange = null;

    @ObjectField(
            configFieldName = ClientConfig.KEY,
            useSubObjectDefault = true,
            ref = OAuthTokenKeyConfig.class,
            description = "light-oauth2 key distribution endpoint configuration for token verification"
    )
    @JsonProperty(ClientConfig.KEY)
    private OAuthTokenKeyConfig key = null;

    public OauthTokenCacheConfig getCache() {
        return cache;
    }

    public Integer getTokenRenewBeforeExpired() {
        return tokenRenewBeforeExpired;
    }

    public Integer getExpiredRefreshRetryDelay() {
        return expiredRefreshRetryDelay;
    }

    public Integer getEarlyRefreshRetryDelay() {
        return earlyRefreshRetryDelay;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public Integer getProxyPort() {
        return proxyPort;
    }

    public Boolean isEnableHttp2() {
        return enableHttp2;
    }

    public OAuthTokenAuthorizationCodeConfig getAuthorizationCode() {
        return authorizationCode;
    }

    public OAuthTokenClientCredentialConfig getClientCredentials() {
        return clientCredentials;
    }

    public OAuthTokenRefreshTokenConfig getRefresh_token() {
        return refreshToken;
    }

    public OAuthTokenExchangeConfig getToken_exchange() { return tokenExchange; }

    public OAuthTokenKeyConfig getKey() {
        return key;
    }

}
