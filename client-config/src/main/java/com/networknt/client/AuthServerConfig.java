package com.networknt.client;

import com.networknt.config.schema.ArrayField;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.StringField;

import java.util.List;

public class AuthServerConfig {

    @StringField(
            configFieldName = "server_url"
    )
    private String server_url;

    @BooleanField(
            configFieldName = "enableHttp2"
    )
    private boolean enableHttp2;

    @StringField(
            configFieldName = "uri"
    )
    private String uri;

    @StringField(
            configFieldName = "client_id"
    )
    private char[] client_id;

    @StringField(
            configFieldName = "client_secret"
    )
    private char[] client_secret;

    @ArrayField(
            configFieldName = "scope",
            items = String.class
    )
    private List<String> scope;


    public String getServer_url() {
        return server_url;
    }

    public boolean isEnableHttp2() {
        return enableHttp2;
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

    public List<String> getScope() {
        return scope;
    }
}
