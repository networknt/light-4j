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

public interface Interceptor extends MiddlewareHandler {

    String CONTENT_TYPE_MISMATCH = "ERR10015";
    char JSON_MAP_OBJECT_STARTING_CHAR = '{';
    char JSON_ARRAY_OBJECT_STARTING_CHAR = '[';


    /**
     * Checks to see if the request is text/xml, text/plain, or application/xml.
     *
     * @param contentType - Content-Type header from exchange
     * @return - true/false
     */
    default boolean isXmlData(String contentType) {
        return contentType.startsWith(ContentType.TEXT_PLAIN.value())
                || contentType.startsWith(ContentType.XML.value())
                || contentType.startsWith(ContentType.APPLICATION_XML.value());
    }

    /**
     * Checks to see if the request is multipart/form-data or x-www-form-urlencoded.
     *
     * @param contentType - Content-Type header from exchange
     * @return - true/false
     */
    default boolean isFormData(String contentType) {
        return contentType.startsWith(ContentType.MULTIPART_FORM_DATA.value())
                || contentType.startsWith(ContentType.APPLICATION_FORM_URLENCODED.value());
    }

    /**
     * Checks to see if the request is application/json.
     *
     * @param contentType - Content-Type header from exchange.
     * @return - true/false
     */
    default boolean isJsonData(String contentType) {
        return contentType.startsWith(ContentType.APPLICATION_JSON.value());
    }

    /**
     * Check to see if the current request/response is compatible to have body data extracted.
     *
     * @param headers - http headers (request/response headers)
     * @return - return true if we should run the body parser.
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

    default boolean parseJsonMapObject(HttpServerExchange ex, final AttachmentKey<Object> key, String str) {
        try {
            ex.putAttachment(key, Config.getInstance().getMapper().readValue(str, new TypeReference<Map<String, Object>>() {}));
            return true;
        } catch (JsonProcessingException e) {
            setExchangeStatus(ex, CONTENT_TYPE_MISMATCH, ContentType.APPLICATION_JSON.value());
            return false;
        }
    }

    default boolean parseJsonArrayObject(final HttpServerExchange ex, final AttachmentKey<Object> key, String str) {
        try {
            ex.putAttachment(key, Config.getInstance().getMapper().readValue(str, new TypeReference<List<Object>>() {}));
            return true;
        } catch (JsonProcessingException e) {
            setExchangeStatus(ex, CONTENT_TYPE_MISMATCH, ContentType.APPLICATION_JSON.value());
            return false;
        }
    }

    default Optional<String> findMatchingPrefix(String url, List<String> prefixes) {
        return prefixes.stream()
                .filter(prefix -> {
                    String[] parts = prefix.split(" ", 2);
                    return url.startsWith(parts[0]);
                })
                .findFirst();
    }

}
