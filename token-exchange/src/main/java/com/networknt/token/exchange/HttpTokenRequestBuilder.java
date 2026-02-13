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
import java.util.HashMap;
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

        if (!headers.isEmpty()) {

            LOG.trace("Adding headers to token request.");

            for (final var header : headers.entrySet()) {
                LOG.trace("Header Key = {} Header Value = {}", header.getKey(), header.getValue());
                this.httpRequestBuilder.header(header.getKey(), String.valueOf(header.getValue()));
            }
        }
        return this;
    }

    public HttpTokenRequestBuilder withBody(final Map<String, String> body, final String type) {

        if (!body.isEmpty()) {

            LOG.trace("Adding body to token request.");

            final var parameters = new HashMap<String, String>();
            for (final var entry : body.entrySet()) {
                LOG.trace("Body key = {} Body value = {}", entry.getKey(), entry.getValue());
                parameters.put(entry.getKey(), String.valueOf(entry.getValue()));
            }

            final String jsonBody;
            if (type.equals("application/x-www-form-urlencoded")) {
                LOG.trace("Formatting body as form data.");
                jsonBody = parameters.entrySet()
                        .stream()
                        .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                        .collect(Collectors.joining("&"));



            } else {
                LOG.trace("Formatting body as JSON.");
                try {
                    jsonBody = Config.getInstance().getMapper().writeValueAsString(parameters);
                } catch (JsonProcessingException e) {
                    throw new IllegalArgumentException("Provided body parameters contain invalid JSON properties.");
                }
            }
            LOG.trace("RequestBody = {}", jsonBody);

            /* only POST requests are supported right now. */
            this.httpRequestBuilder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        }
        return this;
    }

    public HttpRequest build() {
        return this.httpRequestBuilder.build();
    }
}
