package com.networknt.token.exchange;

import java.util.*;

/**
 * Represents the context of an incoming request, containing headers and query parameters.
 * This allows shared variables to reference request data using the !ref(request.*) pattern.
 */
public class RequestContext {

    private final Map<String, String> headers;
    private final Map<String, String> queryParams;
    private final String path;

    private RequestContext(final Map<String, String> headers, final Map<String, String> queryParams, final String path) {
        this.headers = headers != null ? normalizeKeys(headers) : new HashMap<>();
        this.queryParams = queryParams != null ? normalizeKeys(queryParams) : new HashMap<>();
        this.path = path;
    }

    /**
     * Creates an empty request context.
     */
    public static RequestContext empty() {
        return new RequestContext(null, null, null);
    }

    /**
     * Creates a request context with the specified headers, query params, and path.
     */
    public static RequestContext of(final Map<String, String> headers, final Map<String, String> queryParams, final String path) {
        return new RequestContext(headers, queryParams, path);
    }

    /**
     * Creates a request context from an object map (as received by rule engine actions).
     */
    @SuppressWarnings("unchecked")
    public static RequestContext fromObjectMap(final Map<String, Object> objMap) {
        if (objMap == null) {
            return empty();
        }

        final var headers = extractHeaders(objMap);
        final var queryParams = extractQueryParams(objMap);
        final var path = extractPath(objMap);

        return new RequestContext(headers, queryParams, path);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> extractHeaders(final Map<String, Object> objMap) {
        Object headers = objMap.get("requestHeaders");
        if (headers == null) {
            headers = objMap.get("headers");
        }

        if (headers instanceof Map) {
            final var result = new HashMap<String, String>();
            for (final var entry : ((Map<String, Object>) headers).entrySet()) {
                final var value = entry.getValue();
                if (value instanceof String) {
                    result.put(entry.getKey(), (String) value);
                } else if (value instanceof List && !((List<?>) value).isEmpty()) {
                    result.put(entry.getKey(), String.valueOf(((List<?>) value).get(0)));
                }
            }
            return result;
        }
        return new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> extractQueryParams(final Map<String, Object> objMap) {
        Object params = objMap.get("queryParameters");
        if (params == null) {
            params = objMap.get("queryParams");
        }

        if (params instanceof Map) {
            final var result = new HashMap<String, String>();
            for (final var entry : ((Map<String, Object>) params).entrySet()) {
                final var value = entry.getValue();
                if (value instanceof String) {
                    result.put(entry.getKey(), (String) value);
                } else if (value instanceof List && !((List<?>) value).isEmpty()) {
                    result.put(entry.getKey(), String.valueOf(((List<?>) value).get(0)));
                }
            }
            return result;
        }
        return new HashMap<>();
    }

    private static String extractPath(final Map<String, Object> objMap) {
        final var path = objMap.get("requestPath");
        if (path instanceof String) {
            return (String) path;
        }
        final var uri = objMap.get("requestUri");
        if (uri instanceof String) {
            return (String) uri;
        }
        return null;
    }

    /**
     * Normalizes map keys to lowercase for case-insensitive lookup.
     */
    private static Map<String, String> normalizeKeys(final Map<String, String> map) {
        final var normalized = new HashMap<String, String>();
        for (final var entry : map.entrySet()) {
            normalized.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
        }
        return normalized;
    }

    /**
     * Gets a header value (case insensitive).
     */
    public String getHeader(final String name) {
        if (name == null) {
            return null;
        }
        return headers.get(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Gets a query parameter value (case insensitive).
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

