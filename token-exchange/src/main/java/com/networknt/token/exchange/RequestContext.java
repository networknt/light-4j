package com.networknt.token.exchange;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public record RequestContext(String schemaKey, Map<String, String> headers, Map<String, String> queryParams, String path) {

    public RequestContext(String schemaKey, Map<String, String> headers, Map<String, String> queryParams, String path) {
        this.schemaKey = schemaKey;
        this.path = path;
        this.headers = headers == null ? Collections.emptyMap() : normalizeKeys(headers);
        this.queryParams = queryParams == null ? Collections.emptyMap() : normalizeKeys(queryParams);
    }

    private static Map<String, String> normalizeKeys(final Map<String, String> source) {
        Map<String, String> normalized = new HashMap<>();
        for (Map.Entry<String, String> entry : source.entrySet()) {
            String key = entry.getKey();
            String normalizedKey = key == null ? null : key.toLowerCase(Locale.ROOT);
            normalized.put(normalizedKey, entry.getValue());
        }
        return normalized;
    }
    /**
     * Parser interface for extracting RequestContext and applying updates.
     * Implementations determine their own strategy for resolving schema keys.
     */
    public interface Parser {
        /**
         * Parse the incoming request and determine which token schema to use.
         * Implementation is free to use any strategy (path-based, client-id based, etc.)
         *
         * @return RequestContext with resolved schemaKey, or null if no schema should be applied
         */
        RequestContext parseContext();

        /**
         * Apply the transformed token data back to the original request.
         *
         * @param resultMap containing resolved headers/body from TokenExchangeService
         */
        void updateRequest(Map<String, Object> resultMap);
    }

    /**
     * Gets a header value (case-insensitive).
     */
    public String getHeader(final String name) {
        if (name == null) {
            return null;
        }
        return headers.get(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Gets a query parameter value (case-insensitive).
     */
    public String getQueryParam(final String name) {
        if (name == null) {
            return null;
        }
        return queryParams.get(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Gets the request path.
     */
    public String getPath() {
        return path;
    }

    /**
     * Gets all headers.
     */
    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    /**
     * Gets all query parameters.
     */
    public Map<String, String> getQueryParams() {
        return Collections.unmodifiableMap(queryParams);
    }

    /**
     * Gets the Authorization header value.
     */
    public String getAuthorizationHeader() {
        return getHeader("authorization");
    }
}
