## client module

The client module is the key component for light-4j microservice platform which provide API client side call features.

### Overview

Light-4j client supports both HTTP 1.1 and HTTP 2.0 transparently depending on if the target server supports HTTP 2.0 and is used to call APIs from the following sources:

- Web Server
- Standalone Application/Mobile Application
- API/Service

It provides methods to get authorization tokens and automatically receives the client credentials token for scopes in API to API calls. It also helps to pass correlationId and traceabilityId and other configurable headers to the next service.

HTTP 2.0 is multiplex that one connection can send multiple requests at the same time. There is no need to build a connection pool if only HTTP 2.0 is used. However, some users have services that only support HTTP 1.1 without an immediate path to upgrade. To support those users, we have added a connection pool in the Http2Clien 


### Configuration

Setup the client and registry on the service.yml config file (or on centralized config file values.yml).

For example, if you are using consul client &  registry:


```yaml

- com.networknt.registry.URL:
  - com.networknt.registry.URLImpl:
      parameters:
        registryRetryPeriod: '30000'
- com.networknt.consul.client.ConsulClient:
  - com.networknt.consul.client.ConsulClientImpl
- com.networknt.registry.Registry:
  - com.networknt.consul.ConsulRegistry
- com.networknt.balance.LoadBalance:
  - com.networknt.balance.RoundRobinLoadBalance
- com.networknt.cluster.Cluster:
  - com.networknt.cluster.LightCluster


```

For client module config file client.yml, please refer to detail:

https://doc.networknt.com/tutorial/client/configuration/#clientyml


### Content type support

light-4j client module support various http request/response content types. By default, the content type will string base type which include:

    - APPLICATION_JSON("application/json")

    - XML("text/xml")

    - APPLICATION_XML_VALUE("application/xml")
    
    - APPLICATION_FORM_URLENCODED_VALUE("application/x-www-form-urlencoded")
    
    - TEXT_PLAIN_VALUE("text/plain")

For binary data base content types:

    - APPLICATION_PDF_VALUE("application/pdf")

    - IMAGE_PNG_VALUE ("image/png")
    
    - IMAGE_JPEG_VALUE ("image/jpeg")
    
    - IMAGE_GIF_VALUE ("image/gif")

Please refer to the sample in light-example-4j:

https://github.com/networknt/light-example-4j/tree/master/client/pdf



### Usage sample

Client module provides several different approaches for API service call. Here we display three samples below

1. Simple API call directly:

```

        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL,  OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/v1/pets").setMethod(Methods.GET);

            connection.sendRequest(request, client.createClientCallback(reference, latch));

            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
```


2. Use RestClientTemplate class to as client compoment to call service API

```
        RestClientTemplate restClientTemplate = new RestClientTemplate();
        String requestStr = "{\"selection\":{\"accessCard\":\"22222222\",\"selectID\":10009,\"crossReference\":{\"externalSystemID\":226,\"referenceType\":2,\"ID\":\"122222\"}}}";
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("Content-Type", "application/json");
        headerMap.put("Transfer-Encoding", "chunked");

       Map resultMap = restClientTemplate.post("https://localhost:8467", "/networknt/select/",  Map.class, headerMap, requestStr);

```
  
3. Parallel Client call

 Use java 8 + CompletableFuture to handle two or more threads asynchronous computation; We can send requests parallel and process result asynchronously.

 It can better system performance (coroutines, no threads):

 	-- More responsiveness (no blocking on threads)

 	-- More throughput (only bound by CPU)

```
 ServiceDef serviceDef = new ServiceDef(“”https, “com.networknt.petstore1”, null, null);

 Http2ServiceRequest request1 = Http2ServiceRequest(serviceDef, “/get”, HttpVerb.valueOf(“GET”);
 Http2ServiceRequest request2 = Http2ServiceRequest(serviceDef, “/getById/1” “GET”);

 Collection<CompletableFuture<?>> completableFutures = new HashSet<>();
 CompletableFuture<Map> futureResponse1 = request1.callForTypedObject(Map.class);
 CompletableFuture<Map> futureResponse2 = request2.callForTypedObject(Map.class);

 completableFutures.add(futureResponse1);
 completableFutures.add(futureResponse2);
 CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0])).join();

```


### To learn how to use client module, pleases refer to

* [Getting Started](https://doc.networknt.com/concern/client/) to learn core concepts
* [Sample](https://doc.networknt.com/tutorial/rest/openapi/servicemesher/#light-4j-servicemesher-client-module-call-example) with some example code for client module usage
* [Configuration](https://doc.networknt.com/tutorial/client/configuration/#clientyml) for  configuration details
