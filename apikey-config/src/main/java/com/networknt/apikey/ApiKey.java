package com.networknt.apikey;

import com.networknt.config.ConfigInjection;
import com.networknt.config.schema.StringField;

public class ApiKey {

    @StringField(
            configFieldName = "pathPrefix",
            pattern = "^/.*"
    )
    String pathPrefix;

    @StringField(
            configFieldName = "headerName",
            pattern = "^[a-zA-Z0-9-_]*$"
    )
    String headerName;

    @StringField(
            configFieldName = "apiKey"
    )
    String apiKey;

    public String getPathPrefix() {
        return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        // if apiKey is encrypted, we need to decrypted it here.
        this.apiKey = (String)ConfigInjection.decryptEnvValue(ConfigInjection.getDecryptor(), apiKey);
    }
}
