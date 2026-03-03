package com.networknt.client.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.client.Http2Client;
import com.networknt.client.model.HttpVerb;
import com.networknt.client.model.ServiceDef;
import com.networknt.cluster.Cluster;
import com.networknt.common.ContentType;
import com.networknt.config.Config;
import com.networknt.monad.Failure;
import com.networknt.monad.Result;
import com.networknt.monad.Success;
import com.networknt.service.SingletonServiceFactory;
import com.networknt.status.HttpStatus;
import com.networknt.status.Status;
import io.undertow.client.ClientRequest;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * A wrapper for ClientRequest to handle HTTP/2 service requests.
 */
public class Http2ServiceRequest {

    private static Cluster cluster = SingletonServiceFactory.getBean(Cluster.class);
    private final URI hostURI;
    private Optional<String> requestBody = Optional.empty();
    private Boolean addCCToken = false;
    private String authToken;
    /**
     * Error status message constant
     */
    public static final String STATUS_MESSAGE_ERROR = "SERVICE_API_CALL_ERROR";
    /**
     * Default transfer encoding
     */
    public static final String TRANSFER_ENCODING_DEFAULT = "chunked";
    private final ClientRequest clientRequest;

    Http2Client http2Client =Http2Client.getInstance();
    ObjectMapper objectMapper = Config.getInstance().getMapper();

    Optional<List<HttpStatus>> statusCodesValid = Optional.empty();

    /**
     * Constructor with URI and verb.
     * @param uri the URI
     * @param verb the verb
     */
    public Http2ServiceRequest(URI uri, HttpVerb verb) {
        this.hostURI = uri;
        this.clientRequest = new ClientRequest().setMethod(verb.verbHttpString).setPath(uri.getPath());
    }

    /**
     * Constructor with URI and method.
     * @param uri the URI
     * @param method the method
     */
    public Http2ServiceRequest(URI uri, HttpString method) {
        this.hostURI = uri;
        this.clientRequest = new ClientRequest().setMethod(method).setPath(uri.getPath());
    }

    /**
     * Constructor with URI, path, and verb.
     * @param uri the URI
     * @param path the path
     * @param verb the verb
     */
    public Http2ServiceRequest(URI uri, String path, HttpVerb verb) {
        this.hostURI = uri;
        this.clientRequest = new ClientRequest().setMethod(verb.verbHttpString).setPath(path);
    }

    /**
     * Constructor with URI, path, and method.
     * @param uri the URI
     * @param path the path
     * @param method the method
     */
    public Http2ServiceRequest(URI uri, String path, HttpString method) {
        this.hostURI = uri;
        this.clientRequest = new ClientRequest().setMethod(method).setPath(path);
    }

    /**
     * Constructor with ServiceDef, path, and verb.
     * @param serviceDef the service definition
     * @param path the path
     * @param verb the verb
     * @throws URISyntaxException if URI syntax is invalid
     */
    public Http2ServiceRequest(ServiceDef serviceDef, String path, HttpVerb verb) throws URISyntaxException {
        Objects.requireNonNull(cluster);
        this.hostURI = new URI(cluster.serviceToUrl(serviceDef.getProtocol(), serviceDef.getServiceId(), serviceDef.getEnvironment(), serviceDef.getRequestKey()));
        this.clientRequest = new ClientRequest().setMethod(verb.verbHttpString).setPath(path);
    }

    /**
     * Constructor with ServiceDef, path, and method.
     * @param serviceDef the service definition
     * @param path the path
     * @param method the method
     * @throws URISyntaxException if URI syntax is invalid
     */
    public Http2ServiceRequest(ServiceDef serviceDef, String path, HttpString method) throws URISyntaxException {
        Objects.requireNonNull(cluster);
        this.hostURI = new URI(cluster.serviceToUrl(serviceDef.getProtocol(), serviceDef.getServiceId(), serviceDef.getEnvironment(), serviceDef.getRequestKey()));
        this.clientRequest = new ClientRequest().setMethod(method).setPath(path);
    }

    /**
     * Set valid status codes.
     * @param statusCodesValid the list of valid status codes
     */
    public void setStatusCodesValid(List<HttpStatus> statusCodesValid) {
        this.statusCodesValid = Optional.of(statusCodesValid);
    }

    /**
     * Asynchronously call the service.
     * @return a CompletableFuture of Http2ServiceResponse
     */
    public CompletableFuture<Http2ServiceResponse> call() {
        processClientRequest();
        return http2Client.callService(hostURI, clientRequest, requestBody).thenApplyAsync(
                response -> new Http2ServiceResponse(response));
    }

    /**
     * Asynchronously call the service and return a Result.
     * @return a CompletableFuture of Result of Http2ServiceResponse
     */
    public CompletableFuture<Result<Http2ServiceResponse>> callForResult() {
        return this.call().thenComposeAsync(http2ServiceResponse -> {
            CompletableFuture<Result<Http2ServiceResponse>> completableFuture = new CompletableFuture<>();
            try {
                if (http2ServiceResponse.isClientResponseStatusOK()) {
                    completableFuture.complete(Success.of(http2ServiceResponse));
                } else {
                    completableFuture.complete(Failure.of(new Status(http2ServiceResponse.getClientResponseStatusCode(), "ERR500001", STATUS_MESSAGE_ERROR, http2ServiceResponse.getClientResponseBody())));
                }
            } catch (Exception e) {
                completableFuture.completeExceptionally(e);
            }
            return completableFuture;
        });
    }

    /**
     * Asynchronously call the service with a callback.
     * @param callback the callback
     * @param exceptionHandler the exception handler
     * @return a CallWaiter
     */
    public CallWaiter call(Consumer<Http2ServiceResponse> callback, Consumer<Exception> exceptionHandler) {
        return new CallWaiter(this.call().thenAcceptAsync(http2ServiceResponse -> {
            try {
                callback.accept(http2ServiceResponse);
            } catch (Exception e) {
                exceptionHandler.accept(e);
            }
        }), exceptionHandler);
    }

    /**
     * Optionally validate the client response status code.
     * @param statusCode the status code
     * @return true if valid
     * @throws Exception if validation fails
     */
    public boolean optionallyValidateClientResponseStatusCode(int statusCode) throws Exception {
        HttpStatus httpStatus = HttpStatus.resolve(statusCode);
        if (this.statusCodesValid.isPresent() && httpStatus!=null) {
            if (!this.statusCodesValid.get().contains(httpStatus)) {
                return false;
            }
        } else {
            if ((httpStatus!=null && httpStatus.isError()) || (httpStatus==null && statusCode>=400) ) return false;
        }
        return true;
    }

    /**
     * Call for a typed object response.
     * @param <ResponseType> the type of response
     * @param responseTypeClass the class of response
     * @return a CompletableFuture of ResponseType
     */
    public <ResponseType> CompletableFuture<ResponseType> callForTypedObject(Class<ResponseType> responseTypeClass) {
        return this.call().thenComposeAsync(http2ServiceResponse -> {
            CompletableFuture<ResponseType> completableFuture = new CompletableFuture<>();
            try {
                if (optionallyValidateClientResponseStatusCode(http2ServiceResponse.getClientResponseStatusCode())) {
                    completableFuture.complete(http2ServiceResponse.getTypedClientResponse(responseTypeClass));
                } else {
                      throw new Exception("Response code is " + http2ServiceResponse.getClientResponseStatusCode() + "; Error response:" + http2ServiceResponse.getClientResponseBody());
                }
            } catch (Exception e) {
                completableFuture.completeExceptionally(e);
            }
            return completableFuture;
        });
    }

    /**
     * Call for a typed object response with a callback.
     * @param <ResponseType> the type of response
     * @param responseTypeClass the class of response
     * @param callback the callback
     * @param exceptionHandler the exception handler
     * @return a CallWaiter
     */
    public <ResponseType> CallWaiter callForTypedObject(Class<ResponseType> responseTypeClass, Consumer<ResponseType> callback, Consumer<Exception> exceptionHandler) {
        return new CallWaiter(this.call().thenAcceptAsync(http2ServiceResponse -> {
            try {
                if (optionallyValidateClientResponseStatusCode(http2ServiceResponse.getClientResponseStatusCode())) {
                    callback.accept(http2ServiceResponse.getTypedClientResponse(responseTypeClass));
                } else {
                    throw new Exception("Response code is " + http2ServiceResponse.getClientResponseStatusCode() + "; Error response:" + http2ServiceResponse.getClientResponseBody());
                }
            } catch (Exception e) {
                exceptionHandler.accept(e);
            }
        }), exceptionHandler);
    }

    /**
     * Call for a typed list response.
     * @param <ResponseType> the type of response
     * @param responseTypeClass the class of response
     * @return a CompletableFuture of List of ResponseType
     */
    public <ResponseType> CompletableFuture<List<ResponseType>> callForTypedList(Class<ResponseType> responseTypeClass) {
        return this.call().thenComposeAsync(http2ServiceResponse -> {
            CompletableFuture<List<ResponseType>> completableFuture = new CompletableFuture<>();
            try {
                if (optionallyValidateClientResponseStatusCode(http2ServiceResponse.getClientResponseStatusCode())) {
                    completableFuture.complete(http2ServiceResponse.getTypedListClientResponse(responseTypeClass));
                } else {
                    throw new Exception("Response code is " + http2ServiceResponse.getClientResponseStatusCode() + "; Error response:" + http2ServiceResponse.getClientResponseBody());
                }
            } catch (Exception e) {
                completableFuture.completeExceptionally(e);
            }
            return completableFuture;
        });
    }

    /**
     * Call for a typed list response with a callback.
     * @param <ResponseType> the type of response
     * @param responseTypeClass the class of response
     * @param callback the callback
     * @param exceptionHandler the exception handler
     * @return a CallWaiter
     */
    public <ResponseType> CallWaiter callForTypedList(Class<ResponseType> responseTypeClass, Consumer<List<ResponseType>> callback, Consumer<Exception> exceptionHandler) {
        return new CallWaiter(this.call().thenAcceptAsync(http2ServiceResponse -> {
            try {
                if (optionallyValidateClientResponseStatusCode(http2ServiceResponse.getClientResponseStatusCode())) {
                    callback.accept(http2ServiceResponse.getTypedListClientResponse(responseTypeClass));
                } else {
                    throw new Exception("Response code is " + http2ServiceResponse.getClientResponseStatusCode() + "; Error response:" + http2ServiceResponse.getClientResponseBody());
                }
            } catch (Exception e) {
                exceptionHandler.accept(e);
            }
        }), exceptionHandler);
    }

    /**
     * Inner class to wait for the call to complete.
     */
    public static class CallWaiter {
        private final CompletableFuture<Void> future;
        private final Consumer<Exception> exceptionHandler;

        /**
         * Constructor.
         * @param future the future
         * @param exceptionHandler the exception handler
         */
        public CallWaiter(CompletableFuture<Void> future, Consumer<Exception> exceptionHandler) {
            this.future = future;
            this.exceptionHandler = exceptionHandler;
        }

        /**
         * Wait for the response.
         */
        public void waitForResponse() {
            try {
                future.get();
            } catch (Exception e) {
                this.exceptionHandler.accept(e);
            }
        }
    }


    /**
     * Set the request body.
     * @param requestBody the request body object
     * @return the Http2ServiceRequest
     * @throws Exception if serialization fails
     */
    public Http2ServiceRequest setRequestBody(Object requestBody) throws Exception {
        this.requestBody = Optional.ofNullable(this.objectMapper.writeValueAsString(requestBody));
        return this;
    }

    /**
     * Set the request body as string.
     * @param requestBody the request body string
     * @return the Http2ServiceRequest
     */
    public Http2ServiceRequest setRequestBody(String requestBody) {
        if (requestBody!=null)  this.requestBody = Optional.ofNullable(requestBody);
        return this;
    }

    /**
     * Set request headers.
     * @param headerMap the header map
     * @return the Http2ServiceRequest
     */
    public Http2ServiceRequest setRequestHeaders( Map<String, ?> headerMap) {
        if (headerMap!=null) {
            headerMap.forEach((k,v)->this.clientRequest.getRequestHeaders().add(new HttpString(k), v.toString()));
        }
        return this;
    }

    /**
     * Add a request header.
     * @param headerName the header name
     * @param headerValue the header value
     * @return the Http2ServiceRequest
     */
    public Http2ServiceRequest addRequestHeader(String headerName, String headerValue) {
        this.clientRequest.getRequestHeaders().put(new HttpString(headerName), headerValue);
        return this;
    }

    /**
     * Add a request header.
     * @param headerName the header name
     * @param headerValue the header value
     * @return the Http2ServiceRequest
     */
    public Http2ServiceRequest addRequestHeader(HttpString headerName, String headerValue) {
        this.clientRequest.getRequestHeaders().put(headerName, headerValue);
        return this;
    }

    /**
     * Add a request header.
     * @param headerName the header name
     * @param headerValue the header value
     * @return the Http2ServiceRequest
     */
    public Http2ServiceRequest addRequestHeader(String headerName, int headerValue) {
        this.clientRequest.getRequestHeaders().put(new HttpString(headerName), headerValue);
        return this;
    }

    /**
     * Add client credentials token.
     * @return the Http2ServiceRequest
     */
    public Http2ServiceRequest addCCToken() {
        this.addCCToken = true;
        return this;
    }
    /**
     * Set the auth token.
     * @param authToken the auth token
     * @return the Http2ServiceRequest
     */
    public Http2ServiceRequest setAuthToken(String authToken) {
        this.authToken = authToken;
        return this;
    }
    /**
     * Get the auth token.
     * @return the auth token
     */
    public String getAuthToken() {
        return this.authToken;
    }

    private void processClientRequest() {
        if (authToken!=null&&!authToken.isEmpty()) {
            //TODO integrate the client module httpsClient
           // http2Client.addAuthToken(clientRequest, authToken);
        } else {
            if (addCCToken) {
                //TODO integrate the client module httpsClient
            //    http2Client.addCcToken(clientRequest);
            }
        }

        if (this.requestBody!=null) {
            if (this.clientRequest.getRequestHeaders().get(Headers.CONTENT_TYPE) == null) {
                clientRequest.getRequestHeaders().put(Headers.CONTENT_TYPE, ContentType.APPLICATION_JSON.value());
            }
            if (this.clientRequest.getRequestHeaders().get(Headers.TRANSFER_ENCODING) == null) {
                clientRequest.getRequestHeaders().put(Headers.TRANSFER_ENCODING, TRANSFER_ENCODING_DEFAULT);
            }
        }

        // Ensure host header exists
        if (this.clientRequest.getRequestHeaders().get(Headers.HOST) == null ||
                this.clientRequest.getRequestHeaders().get(Headers.HOST).equals("")) {
            String hostHeader = this.hostURI.getHost();
            clientRequest.getRequestHeaders().put(Headers.HOST, hostHeader);
        }
    }

    /**
     * Get the client request.
     * @return the client request
     */
    public ClientRequest getClientRequest() {
        return clientRequest;
    }
}
