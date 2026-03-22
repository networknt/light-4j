package com.networknt.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.common.ContentType;
import com.networknt.config.Config;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interceptor interface that extends MiddlewareHandler to provide common utility methods
 * for request and response interception.
 */
public interface Interceptor extends MiddlewareHandler {

    /** Error code for content type mismatch */
    String CONTENT_TYPE_MISMATCH = "ERR10015";
    /** Constant for JSON map object starting character */
    char JSON_MAP_OBJECT_STARTING_CHAR = '{';
    /** Constant for JSON array object starting character */
    char JSON_ARRAY_OBJECT_STARTING_CHAR = '[';


    /**
     * Checks to see if the request is text/xml, text/plain, or application/xml.
     *
     * @param contentType Content-Type header from exchange.
     * @return true if the content type is XML related, false otherwise.
     */
    default boolean isXmlData(String contentType) {
        return contentType.startsWith(ContentType.TEXT_PLAIN.value())
                || contentType.startsWith(ContentType.XML.value())
                || contentType.startsWith(ContentType.APPLICATION_XML.value());
    }

    /**
     * Checks to see if the request is multipart/form-data or x-www-form-urlencoded.
     *
     * @param contentType Content-Type header from exchange.
     * @return true if the content type is form data related, false otherwise.
     */
    default boolean isFormData(String contentType) {
        return contentType.startsWith(ContentType.MULTIPART_FORM_DATA.value())
                || contentType.startsWith(ContentType.APPLICATION_FORM_URLENCODED.value());
    }

    /**
     * Checks to see if the request is application/json.
     *
     * @param contentType Content-Type header from exchange.
     * @return true if the content type is JSON, false otherwise.
     */
    default boolean isJsonData(String contentType) {
        return contentType.startsWith(ContentType.APPLICATION_JSON.value());
    }

    /**
     * Check to see if the current request/response is compatible to have body data extracted.
     *
     * @param headers http headers (request/response headers)
     * @return true if we should run the body parser, false otherwise.
     */
    default boolean shouldAttachBody(final HeaderMap headers) {
        var hasContentTypeHeader = headers.getFirst(Headers.CONTENT_TYPE) != null;
        var hasData = false;

        if (hasContentTypeHeader) {
            var contentType = headers.getFirst(Headers.CONTENT_TYPE);
            hasData = this.isJsonData(contentType) || this.isFormData(contentType) || this.isXmlData(contentType);
        }

        return hasContentTypeHeader && hasData;
    }

    /**
     * Parses a JSON string into a Map and attaches it to the exchange.
     * A default interface method to get the buffer from the exchange attachment for response body.
     *
     * @param ex  The HttpServerExchange.
     * @param key The attachment key.
     * @param str The JSON string to parse.
     * @return true if successful, false otherwise.
     */
    default boolean parseJsonMapObject(HttpServerExchange ex, final AttachmentKey<Object> key, String str) {
        try {
            ex.putAttachment(key, Config.getInstance().getMapper().readValue(str, new TypeReference<Map<String, Object>>() {}));
            return true;
        } catch (JsonProcessingException e) {
            setExchangeStatus(ex, CONTENT_TYPE_MISMATCH, ContentType.APPLICATION_JSON.value());
            return false;
        }
    }

    /**
     * Parses a JSON string into a List and attaches it to the exchange.
     *
     * @param ex  The HttpServerExchange.
     * @param key The attachment key.
     * @param str The JSON string to parse.
     * @return true if successful, false otherwise.
     */
    default boolean parseJsonArrayObject(final HttpServerExchange ex, final AttachmentKey<Object> key, String str) {
        try {
            ex.putAttachment(key, Config.getInstance().getMapper().readValue(str, new TypeReference<List<Object>>() {}));
            return true;
        } catch (JsonProcessingException e) {
            setExchangeStatus(ex, CONTENT_TYPE_MISMATCH, ContentType.APPLICATION_JSON.value());
            return false;
        }
    }

    /**
     * Finds a matching prefix for a given URL from a list of prefixes.
     *
     * @param url      The URL to check.
     * @param prefixes The list of prefixes.
     * @return An Optional containing the matching prefix, or empty if none match.
     */
    default Optional<String> findMatchingPrefix(String url, List<String> prefixes) {
        return prefixes.stream()
                .filter(url::startsWith)
                .findFirst();
    }

}
