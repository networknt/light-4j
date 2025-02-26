package com.networknt.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.MapLoadable;
import com.networknt.config.schema.ArrayField;
import com.networknt.config.schema.MapField;
import com.networknt.config.schema.StringField;

import java.util.List;
import java.util.Map;

public class OAuthTokenClientCredentialConfig /*implements MapLoadable*/ {


    public static final String URI = "uri";
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String SCOPE = "scope";
    public static final String SERVICE_ID_AUTH_SERVERS = "serviceIdAuthServers";

    @StringField(
            configFieldName = URI,
            externalizedKeyName = "tokenCcUri",
            externalized = true,
            defaultValue = "/oauth2/token",
            description = "token endpoint for client credentials grant"
    )
    private String uri;

    @StringField(
            configFieldName = CLIENT_ID,
            externalizedKeyName = "tokenCcClientId",
            externalized = true,
            defaultValue = "f7d42348-c647-4efb-a52d-4c5787421e72",
            description = "client_id for client credentials grant flow."
    )
    private char[] client_id;

    @StringField(
            configFieldName = CLIENT_SECRET,
            externalizedKeyName = "tokenCcClientSecret",
            externalized = true,
            defaultValue = "f6h1FTI8Q3-7UScPZDzfXA",
            description = "client_secret for client credentials grant flow."
    )
    private char[] client_secret;

    @ArrayField(
            configFieldName = SCOPE,
            externalizedKeyName = "tokenCcScope",
            externalized = true,
            items = String.class,
            description = "optional scope, default scope in the client registration will be used if not defined.\n" +
                    "If there are scopes specified here, they will be verified against the registered scopes.\n" +
                    "In values.yml, you define a list of strings for the scope(s)."
    )
    private List<String> scope;

    @MapField(
            configFieldName = SERVICE_ID_AUTH_SERVERS,
            externalizedKeyName = "tokenCcServiceIdAuthServers",
            valueType = AuthServerConfig.class,
            externalized = true,
            description = "The serviceId to the service specific OAuth 2.0 configuration. " +
                    "Used only when multipleOAuthServer is\n" +
                    "set as true. For detailed config options, please see the values.yml in the client module test."
    )
    private Map<String, AuthServerConfig> serviceIdAuthServers;

    public String getUri() {
        return uri;
    }

    public char[] getClient_id() {
        return client_id;
    }

    public char[] getClient_secret() {
        return client_secret;
    }

    public List<String> getScope() {
        return scope;
    }

    public Map<String, AuthServerConfig> getServiceIdAuthServers() {
        return serviceIdAuthServers;
    }

//    @Override
//    public void loadData(Map<String, Object> data) {
//        if (data.containsKey(URI)) {
//            uri = (String) data.get(URI);
//        }
//
//        if (data.containsKey(CLIENT_ID)) {
//            client_id = ((String) data.get(CLIENT_ID)).toCharArray();
//        }
//
//        if (data.containsKey(CLIENT_SECRET)) {
//            client_secret = ((String) data.get(CLIENT_SECRET)).toCharArray();
//        }
//
//        if (data.containsKey(SCOPE)) {
//            scope = (List<String>) data.get(SCOPE);
//        }
//
//        if (data.containsKey(SERVICE_ID_AUTH_SERVERS)) {
//            final var mapped = (Map<String, Object>) data.get(SERVICE_ID_AUTH_SERVERS);
//            final var mapper = Config.getInstance().getMapper();
//            this.serviceIdAuthServers = mapper.convertValue(mapped, new TypeReference<>() {});
//        }
//    }
}
