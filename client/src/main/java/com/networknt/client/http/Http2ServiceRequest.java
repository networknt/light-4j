package com.networknt.client.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.client.Http2Client;
import com.networknt.client.model.HttpVerb;
import com.networknt.client.model.ServiceDef;
import com.networknt.cluster.Cluster;
import com.networknt.config.Config;
import com.networknt.service.SingletonServiceFactory;
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
import java.util.function.Predicate;

public class Http2ServiceRequest {

    private static Cluster cluster = SingletonServiceFactory.getBean(Cluster.class);
    private final URI hostURI;
    private Optional<String> requestBody = Optional.empty();
    private Boolean addCCToken = false;
    private String authToken;
    private final ClientRequest clientRequest;

    Http2Client http2Client =Http2Client.getInstance();
    ObjectMapper objectMapper = Config.getInstance().getMapper();

    Optional<Predicate<Integer>> isStatusCodeValid = Optional.empty();

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

    public void setIsStatusCodeValid(Predicate<Integer> isStatusCodeValid) {
        this.isStatusCodeValid = Optional.of(isStatusCodeValid);
    }

    public CompletableFuture<Http2ServiceResponse> call() {
        processClientRequest();
        return http2Client.callService(hostURI, clientRequest, requestBody).thenApplyAsync(
                response -> new Http2ServiceResponse(response));
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

    public void optionallyValidateClientResponseStatusCode(int statusCode) throws Exception {
        if (this.isStatusCodeValid.isPresent()) {
            if (!this.isStatusCodeValid.get().test(statusCode)) {
                throw new Exception("cannot type the response object because response code is " + statusCode);
            }
        }
    }

    public <ResponseType> CompletableFuture<ResponseType> callForTypedObject(Class<ResponseType> responseTypeClass) {
        return this.call().thenComposeAsync(http2ServiceResponse -> {
            CompletableFuture<ResponseType> completableFuture = new CompletableFuture<>();
            try {
                optionallyValidateClientResponseStatusCode(http2ServiceResponse.getClientResponseStatusCode());
                completableFuture.complete(http2ServiceResponse.getTypedClientResponse(responseTypeClass));
            } catch (Exception e) {
                completableFuture.completeExceptionally(e);
            }
            return completableFuture;
        });
    }

    public <ResponseType> CallWaiter callForTypedObject(Class<ResponseType> responseTypeClass, Consumer<ResponseType> callback, Consumer<Exception> exceptionHandler) {
        return new CallWaiter(this.call().thenAcceptAsync(http2ServiceResponse -> {
            try {
                optionallyValidateClientResponseStatusCode(http2ServiceResponse.getClientResponseStatusCode());
                callback.accept(http2ServiceResponse.getTypedClientResponse(responseTypeClass));
            } catch (Exception e) {
                exceptionHandler.accept(e);
            }
        }), exceptionHandler);
    }

    public <ResponseType> CompletableFuture<List<ResponseType>> callForTypedList(Class<ResponseType> responseTypeClass) {
        return this.call().thenComposeAsync(http2ServiceResponse -> {
            CompletableFuture<List<ResponseType>> completableFuture = new CompletableFuture<>();
            try {
                optionallyValidateClientResponseStatusCode(http2ServiceResponse.getClientResponseStatusCode());
                completableFuture.complete(http2ServiceResponse.getTypedListClientResponse(responseTypeClass));
            } catch (Exception e) {
                completableFuture.completeExceptionally(e);
            }
            return completableFuture;
        });
    }

    public <ResponseType> CallWaiter callForTypedList(Class<ResponseType> responseTypeClass, Consumer<List<ResponseType>> callback, Consumer<Exception> exceptionHandler) {
        return new CallWaiter(this.call().thenAcceptAsync(http2ServiceResponse -> {
            try {
                optionallyValidateClientResponseStatusCode(http2ServiceResponse.getClientResponseStatusCode());
                callback.accept(http2ServiceResponse.getTypedListClientResponse(responseTypeClass));
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
