package com.networknt.mask;

import com.networknt.config.schema.MapField;

import java.util.Map;

public class MaskRegexConfig {

    @MapField(
            configFieldName = "queryParameter",
            externalizedKeyName = "queryParameter",
            externalized = true,
            valueType = String.class
    )
    private Map<String, String> queryParameter;

    @MapField(
            configFieldName = "requestHeader",
            externalizedKeyName = "requestHeader",
            externalized = true,
            valueType = String.class
    )
    private Map<String, String> requestHeader;

    @MapField(
            configFieldName = "requestCookies",
            externalizedKeyName = "requestCookies",
            externalized = true,
            valueType = String.class
    )
    private Map<String, String> requestCookies;

    @MapField(
            configFieldName = "responseCookies",
            externalizedKeyName = "responseCookies",
            externalized = true,
            valueType = String.class
    )
    private Map<String, String> responseCookies;

    public Map<String, String> getQueryParameter() {
        return queryParameter;
    }

    public Map<String, String> getRequestHeader() {
        return requestHeader;
    }

    public Map<String, String> getRequestCookies() {
        return requestCookies;
    }

    public Map<String, String> getResponseCookies() {
        return responseCookies;
    }
}
