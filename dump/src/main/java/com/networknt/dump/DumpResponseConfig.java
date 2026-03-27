package com.networknt.dump;

import com.networknt.config.schema.ArrayField;
import com.networknt.config.schema.BooleanField;

import java.util.List;

/**
 * Configuration class for dumping responses.
 */
public class DumpResponseConfig {

    /**
     * Default constructor for DumpResponseConfig.
     */
    public DumpResponseConfig() {
    }

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

    /**
     * Checks if response headers are enabled.
     * @return boolean true if enabled
     */
    public boolean isHeaders() {
        return headers;
    }

    /**
     * Checks if response cookies are enabled.
     * @return boolean true if enabled
     */
    public boolean isCookies() {
        return cookies;
    }

    /**
     * Gets filtered cookies.
     * @return List filtered cookies
     */
    public List<String> getFilteredCookies() {
        return filteredCookies;
    }

    /**
     * Gets filtered headers.
     * @return List filtered headers
     */
    public List<String> getFilteredHeaders() {
        return filteredHeaders;
    }

    /**
     * Checks if response body is enabled.
     * @return boolean true if enabled
     */
    public boolean isBody() {
        return body;
    }

    /**
     * Checks if status code is enabled.
     * @return boolean true if enabled
     */
    public boolean isStatusCode() {
        return statusCode;
    }
}
