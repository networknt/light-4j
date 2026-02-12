package com.networknt.token.exchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for resolving variable references in configuration strings.
 * Handles the !ref(*) pattern used throughout token schemas.
 *
 * <p>Supported reference patterns:</p>
 * <ul>
 *   <li>{@code !ref(varName)} - references shared variables (shorthand)</li>
 *   <li>{@code !ref(sharedVariables.varName)} - references shared variables (explicit, for backward compatibility)</li>
 *   <li>{@code !ref(request.header.name)} - references request headers (case insensitive)</li>
 *   <li>{@code !ref(request.query.name)} - references query parameters (case insensitive)</li>
 *   <li>{@code !ref(request.path)} - references the request path</li>
 *   <li>{@code !ref(request.bearerToken)} - references the Bearer token from Authorization header (without "Bearer " prefix)</li>
 * </ul>
 */
public final class VariableResolver {

    private static final Logger LOG = LoggerFactory.getLogger(VariableResolver.class);
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("!ref\\((.*?)\\)");
    private static final String SHARED_VARIABLES_PREFIX = "sharedVariables.";
    private static final String REQUEST_PREFIX = "request.";
    private static final String REQUEST_HEADER_PREFIX = "request.header.";
    private static final String REQUEST_QUERY_PREFIX = "request.query.";
    private static final String REQUEST_PATH = "request.path";
    private static final String REQUEST_BEARER_TOKEN = "request.bearerToken";
    private static final String BEARER_PREFIX = "Bearer ";

    private VariableResolver() {
        // Utility class
    }

    /**
     * Resolves variable references in a string using the provided variables map.
     *
     * @param value     the string potentially containing variable references
     * @param variables the map of variable names to values
     * @return the resolved string with all references replaced
     */
    public static String resolve(final String value, final Map<String, Object> variables) {
        return resolve(value, variables, null);
    }

    /**
     * Resolves variable references in a string using the provided variables map and request context.
     *
     * @param value          the string potentially containing variable references
     * @param variables      the map of variable names to values
     * @param requestContext the request context containing headers and query params (may be null)
     * @return the resolved string with all references replaced
     */
    public static String resolve(final String value, final Map<String, Object> variables, final RequestContext requestContext) {
        if (value == null || variables == null) {
            return value;
        }

        final var matcher = VARIABLE_PATTERN.matcher(value);
        final var result = new StringBuilder();

        while (matcher.find()) {
            final var variablePath = matcher.group(1);
            final var resolvedValue = resolveVariablePath(variablePath, variables, requestContext);

            LOG.trace("Resolving '{}' to '{}'", variablePath, resolvedValue);
            matcher.appendReplacement(result, Matcher.quoteReplacement(resolvedValue));
        }

        return matcher.appendTail(result).toString();
    }

    /**
     * Resolves all variable references in a map's values.
     *
     * @param map       the map with values that may contain variable references
     * @param variables the map of variable names to values
     * @return a new map with all values resolved
     */
    public static Map<String, String> resolveMap(final Map<String, String> map, final Map<String, Object> variables) {
        return resolveMap(map, variables, null);
    }

    /**
     * Resolves all variable references in a map's values using the provided variables and request context.
     *
     * @param map            the map with values that may contain variable references
     * @param variables      the map of variable names to values
     * @param requestContext the request context containing headers and query params (may be null)
     * @return a new map with all values resolved
     */
    public static Map<String, String> resolveMap(final Map<String, String> map, final Map<String, Object> variables, final RequestContext requestContext) {
        if (map == null) {
            return new HashMap<>();
        }

        final var resolved = new HashMap<String, String>();
        for (final var entry : map.entrySet()) {
            resolved.put(entry.getKey(), resolve(entry.getValue(), variables, requestContext));
        }
        return resolved;
    }

    /**
     * Resolves a variable path to its value.
     */
    private static String resolveVariablePath(final String path, final Map<String, Object> variables, final RequestContext requestContext) {
        // Handle request.* references first
        if (path.startsWith(REQUEST_PREFIX)) {
            return resolveRequestVariable(path, requestContext);
        }

        // Everything else is a shared variable (with or without prefix)
        final var variableName = extractVariableName(path);
        return getVariableValue(variables, variableName);
    }

    /**
     * Resolves request context variables (headers, query params, path).
     */
    private static String resolveRequestVariable(final String path, final RequestContext requestContext) {
        if (requestContext == null) {
            LOG.warn("Request context is null, cannot resolve reference: {}", path);
            return "";
        }

        // Handle request.header.* references
        if (path.startsWith(REQUEST_HEADER_PREFIX)) {
            final var headerName = path.substring(REQUEST_HEADER_PREFIX.length());
            final var headerValue = requestContext.getHeader(headerName);
            if (headerValue == null) {
                LOG.warn("Header '{}' not found in request context", headerName);
                return "";
            }
            return headerValue;
        }

        // Handle request.query.* references
        if (path.startsWith(REQUEST_QUERY_PREFIX)) {
            final var paramName = path.substring(REQUEST_QUERY_PREFIX.length());
            final var paramValue = requestContext.getQueryParam(paramName);
            if (paramValue == null) {
                LOG.warn("Query parameter '{}' not found in request context", paramName);
                return "";
            }
            return paramValue;
        }

        // Handle request.path reference
        if (path.equals(REQUEST_PATH)) {
            final var pathValue = requestContext.getPath();
            return pathValue != null ? pathValue : "";
        }

        // Handle request.bearerToken reference - extracts token without "Bearer " prefix
        if (path.equals(REQUEST_BEARER_TOKEN)) {
            final var authHeader = requestContext.getAuthorizationHeader();
            if (authHeader == null) {
                LOG.warn("Authorization header not found in request context");
                return "";
            }
            if (authHeader.startsWith(BEARER_PREFIX)) {
                return authHeader.substring(BEARER_PREFIX.length());
            }
            // Return as-is if no Bearer prefix
            return authHeader;
        }

        LOG.warn("Unknown request reference pattern: {}", path);
        return "";
    }

    /**
     * Extracts the variable name from a path like "sharedVariables.accessToken".
     */
    private static String extractVariableName(final String path) {
        if (path.startsWith(SHARED_VARIABLES_PREFIX)) {
            return path.substring(SHARED_VARIABLES_PREFIX.length());
        }
        return path;
    }

    /**
     * Gets the value of a variable from the map, converting to String as needed.
     */
    private static String getVariableValue(final Map<String, Object> variables, final String name) {
        final var value = variables.get(name);
        if (value == null) {
            LOG.warn("Variable '{}' not found in shared variables", name);
            return "";
        }

        if (value instanceof char[]) {
            return new String((char[]) value);
        }
        return String.valueOf(value);
    }

    /**
     * Extracts the destination variable name from a destination pattern.
     * E.g., "!ref(sharedVariables.accessToken)" returns "accessToken"
     */
    public static String extractDestinationVariable(final String destination) {
        final var matcher = VARIABLE_PATTERN.matcher(destination);
        if (matcher.find()) {
            return extractVariableName(matcher.group(1));
        }
        throw new IllegalArgumentException("Invalid destination pattern: " + destination);
    }
}
