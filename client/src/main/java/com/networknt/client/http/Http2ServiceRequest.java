package com.networknt.client.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.client.Http2Client;
import com.networknt.client.model.HttpVerb;
import com.networknt.client.model.ServiceDef;
import com.networknt.cluster.Cluster;
import com.networknt.config.Config;
import com.networknt.httpstring.ContentType;
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

public class Http2ServiceRequest {

    private static Cluster cluster = SingletonServiceFactory.getBean(Cluster.class);
    private final URI hostURI;
    private Optional<String> requestBody = Optional.empty();
    private Boolean addCCToken = false;
    private String authToken;
    public static final String STATUS_MESSAGE_ERROR = "SERVICE_API_CALL_ERROR";
    public static final String TRANSFER_ENCODING_DEFAULT = "chunked";
    private final ClientRequest clientRequest;

    Http2Client http2Client =Http2Client.getInstance();
    ObjectMapper objectMapper = Config.getInstance().getMapper();

    Optional<List<HttpStatus>> statusCodesValid = Optional.empty();

    public Http2ServiceRequest(URI uri, HttpVerb verb) {
        this.hostURI = uri;
        this.clientRequest = new ClientRequest().setMethod(verb.verbHttpString).setPath(uri.getPath());
    }

    public Http2ServiceRequest(URI uri, HttpString method) {
        this.hostURI = uri;
        this.clientRequest = new ClientRequest().setMethod(method).setPath(uri.getPath());
    }

    public Http2ServiceRequest(URI uri, String path, HttpVerb verb) {
        this.hostURI = uri;
        this.clientRequest = new ClientRequest().setMethod(verb.verbHttpString).setPath(path);
    }

    public Http2ServiceRequest(URI uri, String path, HttpString method) {
        this.hostURI = uri;
        this.clientRequest = new ClientRequest().setMethod(method).setPath(path);
    }

    public Http2ServiceRequest(ServiceDef serviceDef, String path, HttpVerb verb) throws URISyntaxException {
        Objects.requireNonNull(cluster);
        this.hostURI = new URI(cluster.serviceToUrl(serviceDef.getProtocol(), serviceDef.getServiceId(), serviceDef.getEnvironment(), serviceDef.getRequestKey()));
        this.clientRequest = new ClientRequest().setMethod(verb.verbHttpString).setPath(path);
    }

    public Http2ServiceRequest(ServiceDef serviceDef, String path, HttpString method) throws URISyntaxException {
        Objects.requireNonNull(cluster);
        this.hostURI = new URI(cluster.serviceToUrl(serviceDef.getProtocol(), serviceDef.getServiceId(), serviceDef.getEnvironment(), serviceDef.getRequestKey()));
        this.clientRequest = new ClientRequest().setMethod(method).setPath(path);
    }

    public void setStatusCodesValid(List<HttpStatus> statusCodesValid) {
        this.statusCodesValid = Optional.of(statusCodesValid);
    }

    public CompletableFuture<Http2ServiceResponse> call() {
        processClientRequest();
        return http2Client.callService(hostURI, clientRequest, requestBody).thenApplyAsync(
                response -> new Http2ServiceResponse(response));
    }

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

    public CallWaiter call(Consumer<Http2ServiceResponse> callback, Consumer<Exception> exceptionHandler) {
        return new CallWaiter(this.call().thenAcceptAsync(http2ServiceResponse -> {
            try {
                callback.accept(http2ServiceResponse);
            } catch (Exception e) {
                exceptionHandler.accept(e);
            }
        }), exceptionHandler);
    }

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

    public static class CallWaiter {
        private final CompletableFuture<Void> future;
        private final Consumer<Exception> exceptionHandler;

        public CallWaiter(CompletableFuture<Void> future, Consumer<Exception> exceptionHandler) {
            this.future = future;
            this.exceptionHandler = exceptionHandler;
        }

        public void waitForResponse() {
            try {
                future.get();
            } catch (Exception e) {
                this.exceptionHandler.accept(e);
            }
        }
    }


    public Http2ServiceRequest setRequestBody(Object requestBody) throws Exception {
        this.requestBody = Optional.ofNullable(this.objectMapper.writeValueAsString(requestBody));
        return this;
    }


    public Http2ServiceRequest setRequestBody(String requestBody) {
        if (requestBody!=null)  this.requestBody = Optional.ofNullable(requestBody);
        return this;
    }

    public Http2ServiceRequest setRequestHeaders( Map<String, ?> headerMap) {
        if (headerMap!=null) {
            headerMap.forEach((k,v)->this.clientRequest.getRequestHeaders().add(new HttpString(k), v.toString()));
        }
        return this;
    }

    public Http2ServiceRequest addRequestHeader(String headerName, String headerValue) {
        this.clientRequest.getRequestHeaders().put(new HttpString(headerName), headerValue);
        return this;
    }

    public Http2ServiceRequest addRequestHeader(HttpString headerName, String headerValue) {
        this.clientRequest.getRequestHeaders().put(headerName, headerValue);
        return this;
    }

    public Http2ServiceRequest addRequestHeader(String headerName, int headerValue) {
        this.clientRequest.getRequestHeaders().put(new HttpString(headerName), headerValue);
        return this;
    }

    public Http2ServiceRequest addCCToken() {
        this.addCCToken = true;
        return this;
    }
    public Http2ServiceRequest setAuthToken(String authToken) {
        this.authToken = authToken;
        return this;
    }
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

    public ClientRequest getClientRequest() {
        return clientRequest;
    }
}
