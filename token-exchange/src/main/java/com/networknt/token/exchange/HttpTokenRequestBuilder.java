package com.networknt.token.exchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpTokenRequestBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(HttpTokenRequestBuilder.class);

    final HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder();

    public HttpTokenRequestBuilder(final String url) {

        try {
            this.httpRequestBuilder.uri(new URI(url));

        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Provided URL '" + url + "' is invalid.");
        }
    }

    public HttpTokenRequestBuilder withHeaders(final Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return this;
        }

        LOG.trace("Adding headers to token request.");

        for (final var header : headers.entrySet()) {
            LOG.trace("Header Key = {} Header Value = {}", header.getKey(), header.getValue());
            this.httpRequestBuilder.header(header.getKey(), header.getValue());
        }
        return this;
    }

    public HttpTokenRequestBuilder withBody(final Map<String, String> body, final String type) {
        if (body == null || body.isEmpty()) {
            LOG.debug("Body is empty, skipping body configuration.");
            return this;
        }

        if (type == null) {
            throw new IllegalArgumentException("Content-Type must not be null");
        }

        LOG.trace("Adding body to token request with Content-Type: {}", type);

        final String requestBody;
        if ("application/x-www-form-urlencoded".equals(type)) {
            LOG.trace("Formatting body as form data.");
            requestBody = body.entrySet()
                    .stream()
                    .map(e -> {
                        final String key = URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8);
                        final String value = URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8);
                        LOG.trace("Body key = {} Body value = {}", e.getKey(), e.getValue());
                        return key + "=" + value;
                    })
                    .collect(Collectors.joining("&"));

        } else if ("application/json".equals(type)) {
            LOG.trace("Formatting body as JSON.");
            try {
                requestBody = Config.getInstance().getMapper().writeValueAsString(body);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Failed to serialize body parameters to JSON: " + e.getMessage(), e);
            }
        } else {
            throw new IllegalArgumentException("Unsupported Content-Type: " + type +
                ". Supported types: application/json, application/x-www-form-urlencoded");
        }

        LOG.trace("RequestBody = {}", requestBody);

        /* only POST requests are supported right now. */
        this.httpRequestBuilder.POST(HttpRequest.BodyPublishers.ofString(requestBody));
        return this;
    }

    public HttpRequest build() {
        return this.httpRequestBuilder.build();
    }
}
