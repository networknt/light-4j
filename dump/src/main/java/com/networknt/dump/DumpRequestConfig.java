package com.networknt.dump;

import com.networknt.config.schema.ArrayField;
import com.networknt.config.schema.BooleanField;

import java.util.List;

/**
 * Configuration class for dumping requests.
 */
public class DumpRequestConfig {

    /**
     * Default constructor for DumpRequestConfig.
     */
    public DumpRequestConfig() {
    }

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

    /**
     * Checks if URL dumping is enabled.
     * @return boolean true if enabled
     */
    public boolean isUrl() {
        return url;
    }

    /**
     * Checks if header dumping is enabled.
     * @return boolean true if enabled
     */
    public boolean isHeaders() {
        return headers;
    }

    /**
     * Gets filtered headers.
     * @return List filtered headers
     */
    public List<String> getFilteredHeaders() {
        return filteredHeaders;
    }

    /**
     * Checks if cookie dumping is enabled.
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
     * Checks if query parameter dumping is enabled.
     * @return boolean true if enabled
     */
    public boolean isQueryParameters() {
        return queryParameters;
    }

    /**
     * Gets filtered query parameters.
     * @return List filtered query parameters
     */
    public List<String> getFilteredQueryParameters() {
        return filteredQueryParameters;
    }

    /**
     * Checks if body dumping is enabled.
     * @return boolean true if enabled
     */
    public boolean isBody() {
        return body;
    }
}
