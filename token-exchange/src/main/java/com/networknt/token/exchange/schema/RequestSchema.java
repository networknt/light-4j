package com.networknt.token.exchange.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.networknt.config.schema.*;
import com.networknt.token.exchange.RequestContext;
import com.networknt.token.exchange.VariableResolver;
import com.networknt.token.exchange.schema.cert.SSLContextSchema;
import com.networknt.token.exchange.schema.jwt.JwtSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.HashMap;
import java.util.Map;


@JsonIgnoreProperties(value={ "sslContext", "httpRequest", "httpClient", "clientLock" }, allowGetters=true)
public class RequestSchema {

    private static final Logger LOG = LoggerFactory.getLogger(RequestSchema.class);

    private static final String URL = "url";
    private static final String HEADERS = "headers";
    private static final String BODY = "body";
    private static final String TYPE = "type";
    private static final String CACHE_HTTP_CLIENT = "cacheHttpClient";
    private static final String CACHE_SSL_CONTEXT = "cacheSSLContext";
    private static final String PROXY_HOST = "proxyHost";
    private static final String PROXY_PORT = "proxyPort";
    private static final String ENABLE_HTTP2 = "enableHttp2";
    private static final String SSL_CONTEXT_SCHEMA = "sslContextSchema";
    private static final String JWT_SCHEMA = "jwtSchema";

    /** Lock object for thread-safe HTTP client caching */
    private final Object clientLock = new Object();

    @StringField(
            configFieldName = URL,
            description = "The full url to hit the token service."
    )
    @JsonProperty(URL)
    private String url;

    @MapField(
            configFieldName = HEADERS,
            description = "Headers used in the token request.",
            valueType = String.class
    )
    @JsonProperty(HEADERS)
    protected Map<String, String> headers;

    @MapField(
            configFieldName = BODY,
            description = "Body used in the token request.",
            valueType = String.class
    )
    @JsonProperty(BODY)
    protected Map<String, String> body;

    @StringField(
            configFieldName = TYPE,
            description = "The type of the request made. application/json etc."
    )
    @JsonProperty(TYPE)
    private String type;

    @BooleanField(configFieldName = CACHE_HTTP_CLIENT)
    @JsonProperty(CACHE_HTTP_CLIENT)
    private boolean cacheHttpClient;

    @BooleanField(configFieldName = CACHE_SSL_CONTEXT)
    @JsonProperty(CACHE_SSL_CONTEXT)
    private boolean cacheSSLContext;

    @StringField(configFieldName = PROXY_HOST)
    @JsonProperty(PROXY_HOST)
    private String proxyHost;

    @IntegerField(configFieldName = PROXY_PORT)
    @JsonProperty(PROXY_PORT)
    private int proxyPort;

    @BooleanField(configFieldName = ENABLE_HTTP2)
    @JsonProperty(ENABLE_HTTP2)
    private boolean enableHttp2;

    @ObjectField(configFieldName = SSL_CONTEXT_SCHEMA, ref = SSLContextSchema.class)
    @JsonProperty(SSL_CONTEXT_SCHEMA)
    private SSLContextSchema sslContextSchema;

    @ObjectField(configFieldName = JWT_SCHEMA, ref = JwtSchema.class)
    @JsonProperty(JWT_SCHEMA)
    private JwtSchema jwtSchema;

    private HttpRequest httpRequest;
    private HttpClient httpClient;
    private SSLContext sslContext;



    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, String> getBody() {
        return body;
    }

    public boolean isCacheHttpClient() {
        return cacheHttpClient;
    }

    public boolean isCacheSSLContext() {
        return cacheSSLContext;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public boolean isEnableHttp2() {
        return enableHttp2;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public void setBody(Map<String, String> body) {
        this.body = body;
    }

    public String getUrl() {
        return url;
    }
    public SSLContextSchema getSslContextSchema() {
        return sslContextSchema;
    }

    public JwtSchema getJwtSchema() {
        return jwtSchema;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    public String getType() {
        return type;
    }

    public HttpRequest getHttpRequest() {
        return httpRequest;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public Object getClientLock() {
        return clientLock;
    }

    public void setHttpRequest(HttpRequest httpRequest) {
        this.httpRequest = httpRequest;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    public Map<String, String> getResolvedHeaders(final SharedVariableSchema sharedVariableSchema) {
        return getResolvedHeaders(sharedVariableSchema, null);
    }

    public Map<String, String> getResolvedHeaders(final SharedVariableSchema sharedVariableSchema, final RequestContext requestContext) {
        if (this.headers == null) {
            LOG.trace("No headers defined in request schema.");
            return new HashMap<>();
        }
        return VariableResolver.resolveMap(this.headers, sharedVariableSchema.asMap(), requestContext);
    }

    public Map<String, String> getResolvedBody(final SharedVariableSchema sharedVariableSchema) {
        return getResolvedBody(sharedVariableSchema, null);
    }

    public Map<String, String> getResolvedBody(final SharedVariableSchema sharedVariableSchema, final RequestContext requestContext) {
        if (this.body == null) {
            LOG.trace("No body defined in request schema.");
            return new HashMap<>();
        }
        return VariableResolver.resolveMap(this.body, sharedVariableSchema.asMap(), requestContext);
    }
}
