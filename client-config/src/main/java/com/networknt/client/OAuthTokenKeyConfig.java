package com.networknt.client;
import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.MapLoadable;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.MapField;
import com.networknt.config.schema.StringField;

import java.util.Map;

public class OAuthTokenKeyConfig /*implements MapLoadable*/ {

    public static final String SERVER_URL = "server_url";
    public static final String SERVICE_ID = "serviceId";
    public static final String URI = "uri";
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String ENABLE_HTTP_2 = "enableHttp2";
    public static final String SERVICE_ID_AUTH_SERVERS = "serviceIdAuthServers";
    public static final String AUDIENCE = "audience";
    @StringField(
            configFieldName = SERVER_URL,
            externalizedKeyName = "tokenKeyServerUrl",
            externalized = true,
            description = "key distribution server url for token verification. It will be used if it is configured.\n" +
                    "If it is not set, a service lookup will be taken with serviceId to find an instance"
    )
    private String server_url;

    @StringField(
            configFieldName = SERVICE_ID,
            externalizedKeyName = "tokenKeyServiceId",
            externalized = true,
            defaultValue = "com.networknt.oauth2-key-1.0.0",
            description = "key serviceId for key distribution service, " +
                    "it will be used if above server_url is not configured."
    )
    private String serviceId;

    @StringField(
            configFieldName = URI,
            externalizedKeyName = "tokenKeyUri",
            externalized = true,
            defaultValue = "/oauth2/key",
            description = "the path for the key distribution endpoint"
    )
    private String uri;

    @StringField(
            configFieldName = CLIENT_ID,
            externalizedKeyName = "tokenKeyClientId",
            externalized = true,
            defaultValue = "f7d42348-c647-4efb-a52d-4c5787421e72",
            description = "client_id used to access key distribution service. " +
                    "It can be the same client_id with token service or not."
    )
    private char[] client_id;

    @StringField(
            configFieldName = CLIENT_SECRET,
            externalizedKeyName = "tokenKeyClientSecret",
            externalized = true,
            defaultValue = "f6h1FTI8Q3-7UScPZDzfXA",
            description = "client secret used to access the key distribution service."
    )
    private char[] client_secret;

    @BooleanField(
            configFieldName = ENABLE_HTTP_2,
            externalizedKeyName = "tokenKeyEnableHttp2",
            defaultValue = true,
            externalized = true,
            description = "set to true if the oauth2 provider supports HTTP/2"
    )
    private boolean enableHttp2;

    @MapField(
            configFieldName = SERVICE_ID_AUTH_SERVERS,
            externalizedKeyName = "tokenKeyServiceIdAuthServers",
            valueType = AuthServerConfig.class,
            externalized = true,
            description = "The serviceId to the service specific OAuth 2.0 configuration. " +
                    "Used only when multipleOAuthServer is\n" +
                    "set as true. For detailed config options, please see the values.yml in the client module test."
    )
    private Map<String, AuthServerConfig> serviceIdAuthServers;

    @StringField(
            configFieldName = AUDIENCE,
            externalizedKeyName = "tokenKeyAudience",
            externalized = true,
            description = "audience for the token validation. " +
                    "It is optional and if it is not configured, no audience validation will be executed."
    )
    private String audience;

    public String getServer_url() {
        return server_url;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getUri() {
        return uri;
    }

    public char[] getClient_id() {
        return client_id;
    }

    public char[] getClient_secret() {
        return client_secret;
    }

    public boolean isEnableHttp2() {
        return enableHttp2;
    }

    public Map<String, AuthServerConfig> getServiceIdAuthServers() {
        return serviceIdAuthServers;
    }

    public String getAudience() {
        return audience;
    }

//    @Override
//    public void loadData(Map<String, Object> data) {
//        if (data.containsKey(SERVER_URL)) {
//            this.server_url = (String) data.get(SERVER_URL);
//        }
//
//        if (data.containsKey(SERVICE_ID)) {
//            this.serviceId = (String) data.get(SERVICE_ID);
//        }
//
//        if (data.containsKey(URI)) {
//            this.uri = (String) data.get(URI);
//        }
//
//        if (data.containsKey(CLIENT_ID)) {
//            this.client_id = ((String) data.get(CLIENT_ID)).toCharArray();
//        }
//
//        if (data.containsKey(CLIENT_SECRET)) {
//            this.client_secret = ((String) data.get(CLIENT_SECRET)).toCharArray();
//        }
//
//        if (data.containsKey(ENABLE_HTTP_2)) {
//            this.enableHttp2 = Config.loadBooleanValue(ENABLE_HTTP_2, data.get(ENABLE_HTTP_2));
//        }
//
//        if (data.containsKey(AUDIENCE)) {
//            this.audience = (String) data.get(AUDIENCE);
//        }
//
//        if (data.containsKey(SERVICE_ID_AUTH_SERVERS)) {
//            final var mapped = (Map<String, Object>) data.get(SERVICE_ID_AUTH_SERVERS);
//            final var mapper = Config.getInstance().getMapper();
//            this.serviceIdAuthServers = mapper.convertValue(mapped, new TypeReference<>() {});
//        }
//
//
//    }
}
