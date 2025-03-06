package com.networknt.dump;

import com.networknt.config.schema.ArrayField;
import com.networknt.config.schema.BooleanField;

import java.util.List;

public class DumpResponseConfig {

    @BooleanField(
            configFieldName = "headers",
            description = "Dump response headers"
    )
    private boolean headers;

    @ArrayField(
            configFieldName = "filteredHeaders",
            description = "List of headers to filter",
            items = String.class
    )
    private List<String> filteredHeaders;

    @BooleanField(
            configFieldName = "cookies",
            description = "Dump response cookies"
    )
    private boolean cookies;

    @ArrayField(
            configFieldName = "filteredCookies",
            description = "List of cookies to filter",
            items = String.class
    )
    private List<String> filteredCookies;

    @BooleanField(
            configFieldName = "body",
            description = "Dump response body"
    )
    private boolean body;

    @BooleanField(
            configFieldName = "statusCode",
            description = "Dump response status code"
    )
    private boolean statusCode;

    public boolean isHeaders() {
        return headers;
    }

    public boolean isCookies() {
        return cookies;
    }

    public List<String> getFilteredCookies() {
        return filteredCookies;
    }

    public List<String> getFilteredHeaders() {
        return filteredHeaders;
    }

    public boolean isBody() {
        return body;
    }

    public boolean isStatusCode() {
        return statusCode;
    }
}
