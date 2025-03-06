package com.networknt.dump;

import com.networknt.config.schema.ArrayField;
import com.networknt.config.schema.BooleanField;

import java.util.List;

public class DumpRequestConfig {

    @BooleanField(
            configFieldName = "enabled",
            description = "Indicate if the dump request middleware is enabled or not."
    )
    private boolean url;

    @BooleanField(
            configFieldName = "headers",
            description = "Indicate if the headers should be dumped or not."
    )
    private boolean headers;

    @ArrayField(
            configFieldName = "filteredHeaders",
            description = "List of headers that should be filtered out from the dump.",
            items = String.class
    )
    private List<String> filteredHeaders;

    @BooleanField(
            configFieldName = "cookies",
            description = "Indicate if the cookies should be dumped or not."
    )
    private boolean cookies;

    @ArrayField(
            configFieldName = "filteredCookies",
            description = "List of cookies that should be filtered out from the dump.",
            items = String.class
    )
    private List<String> filteredCookies;

    @BooleanField(
            configFieldName = "queryParameters",
            description = "Indicate if the query parameters should be dumped or not."
    )
    private boolean queryParameters;

    @ArrayField(
            configFieldName = "filteredQueryParameters",
            description = "List of query parameters that should be filtered",
            items = String.class
    )
    private List<String> filteredQueryParameters;

    @BooleanField(
            configFieldName = "body",
            description = "Indicate if the body should be dumped or not."
    )
    private boolean body;

    public boolean isUrl() {
        return url;
    }

    public boolean isHeaders() {
        return headers;
    }

    public List<String> getFilteredHeaders() {
        return filteredHeaders;
    }

    public boolean isCookies() {
        return cookies;
    }

    public List<String> getFilteredCookies() {
        return filteredCookies;
    }

    public boolean isQueryParameters() {
        return queryParameters;
    }

    public List<String> getFilteredQueryParameters() {
        return filteredQueryParameters;
    }

    public boolean isBody() {
        return body;
    }
}
