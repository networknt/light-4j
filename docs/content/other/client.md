---
date: 2016-10-23T12:26:20-04:00
title: Client
---

## Introduction

In microservices architecture, service to service communication can be done by
request/response style or messaging/event style. An efficient http client is
crucial in request/response style as the number of interactions between services
are high and extra latency can kill the entire application performance to cause
the failure of microservices application.

In the early day of light-4j we have a client module based on Apache HttpClient
and Apache HttpAsyncClient which supports HTTP 1.1 and is very popular in the
open source community. However, it was designed long ago and it is very hard to
use because too many configurations. It is also very big and slow compare with
other modern http clients. Another big issue is HTTP 2.0 support as light-*-4j
frameworks support HTTP2 natively and we want to take advantage on the client
side as well.

In looking for Java HTTP clients that support HTTP 2.0, I was stuck there as
none of the them support it gracefully. Some are partial support and most of
them require you to put a version of jar file in the command line so work with
Java 8. Everybody seems to waiting for Java 9 to come out but we cannot use it
on production until it is ready.

Given above situations, I decided to implement my own Http2Client based on what
we have in Undertow. I have proposed the idea to implement a generic Http2Client
to Undertow community but it wasn't interested. It will take a long time to build
an independent Http2Client without depending on Undertow and it is OK with us
as our server is based on Undertow. Other people might have a concern on that but
the argument is Undertow core is extremely small and I don't thing it is a big
issue for other people to use it outside of light-*-4j.

I am starting an [http client benchmark](https://github.com/networknt/http2client-benchmark)
and if there are more interests on this client, I will make it an independent
module without depending on Undertow so that other people working on other platforms
can use it without the extra Undertow core.

## Usage

Http2Client supports both HTTP 1.1 and HTTP 2.0 transparently depending on the
server HTTP 2.0 support and is used to call APIs from the following sources:

1. Web Server
2. Standalone Application/Mobile Application
3. API/Service

It provides method to get authorization token and automatically gets client
credentials token for scopes in API to API calls. It also helps to pass correlationId
and traceabilityId and other configurable headers to the next service.

Although it support HTTP 1.1, it requires the user to create a connection pool as
HTTP 1.1 connection doesn't support multiplex and one connection can only handle one
request concurrently. I am planning to add a internal connection pool if there is a
need but currently it is not necessary as all the communication is on HTTP 2.0


### Generic response callback functions

Like Undertow core server, it is event driven with callbacks so non-blocking
all the time to free your CPU for other important computation. Http2Client
has two generic callback functions implemented to handle request with body(POST,
PUT, PATCH) and request without body(GET, DELETE). Users can create their own
customized callback functions to handle the response from the server.

Signature for GET/DELETE

```
public ClientCallback<ClientExchange> createClientCallback(final AtomicReference<ClientResponse> reference, final CountDownLatch latch)
```

Signature for POST, PUT, PATCH

```
public ClientCallback<ClientExchange> createClientCallback(final AtomicReference<ClientResponse> reference, final CountDownLatch latch, final String requestBody)
```

Note that you need to pass in a requestBody for POST, PUT or PATCH.

### Client Credentials token renew and cache
The renew of token happens behind the scene and it supports circuit breaker
if OAuth2 server is down or busy. It renews the token pro-actively before the
current one is expired and lets all requests go with the current token. It only
block other request if the current request is trying to renew an expired token.
When token renewal in this case fails, all request will be rejected with timeout
and subsequent requests the same until a grace period is passed so that the
renew process is start again. 

### Examples

#### Get a client instance

```
    Http2Client client = Http2Client.getInstance();
```

#### Call from web server

To set the header with authorization code JWT token.

```
    public void addAuthToken(HttpRequest request, String token) 

```

To set the header with authorization code JWT token and traceabilityId.

```
    public void addAuthTokenTrace(HttpRequest request, String token, String traceabilityId) 
```

#### Call from standalone app/mobile app

To set the header with client credentials JWT token.
```
    public void addCcToken(HttpRequest request) throws ClientException, ApiException 
```

To set the header with client credentials JWT token and traceabilityId.

```
    public void addCcTokenTrace(HttpRequest request, String traceabilityId) throws ClientException, ApiException 

```

#### call from API/service

To pass in exchange.

```
    public void propagateHeaders(HttpRequest request, final HttpServerExchange exchange) throws ClientException, ApiException 

```

To pass in variables individually.

```
    public void populateHeader(HttpRequest request, String authToken, String correlationId, String traceabilityId) throws ClientException, ApiException 

```

### Test cases that use Http2Client

All tests in light-*-4j frameworks are real test against the real server and the test cases are using
Http2Client to communicate with the server. We have several hundreds test cases in all different frameworks
and light-oauth2 concentrate some of them. Take a look at https://github.com/networknt/light-oauth2/tree/master/client/src/test/java/com/networknt/oauth/client/handler
for examples on how to write test cases with Http2Client. In fact, you can check other modules in light-oauth2
for test cases examples as well.

### Http2Client communicate with third party server.

Within light-4j, there are several places that Http2Client is used to communicate with third party servers

* OAuth2 server

https://github.com/networknt/light-4j/blob/master/client/src/main/java/com/networknt/client/oauth/OauthHelper.java

* Consul

https://github.com/networknt/light-4j/blob/master/consul/src/main/java/com/networknt/consul/client/ConsulEcwidClient.java

* InfluxDB

https://github.com/networknt/light-4j/blob/master/metrics/src/main/java/io/dropwizard/metrics/influxdb/InfluxDbHttpSender.java

### Tutorials that use Http2Client

* Microservices Chain pattern tutorial

https://networknt.github.io/light-rest-4j/tutorial/ms-chain/

* Service discovery tutorial

https://networknt.github.io/light-rest-4j/tutorial/discovery/


### Close connection

In most of the cases, we don't close the connection and all the requests will go through the same
HTTP/2 connection with multiplex support. However, there are times you need to create a new connection
on demand and close it once it is used. For example, in [Server.java](https://github.com/networknt/light-4j/blob/master/server/src/main/java/com/networknt/server/Server.java) 
we create a new connection to load config from light-config-server during startup. In this case,
although our config server supports HTTP/2, we create and close the connection as this is one time
request and we don't want to hold the resource. Here is the lines to close the connection in finally
block.

```java
    } finally {
        IoUtils.safeClose(connection);
    }
```

Another place that we create connection for each request is [ConsulEcwidClient.java](https://github.com/networknt/light-4j/blob/develop/consul/src/main/java/com/networknt/consul/client/ConsulEcwidClient.java) 
in the consul module. The reason is due to consul doesn't support HTTP/2 and all the requests are 
on/off basis. 

### Configuration

When using client module of light-4j, you need to have a configuration file client.yml in
your src/main/resources/config folder. If OAuth 2.0 is used to secure the API access, then
you need to update secret.yml to have client secrets configured there.

The following is the default client.yml in light-4j and it should overwritten in your app.

```yaml
# This is the configuration file for Http2Client.
---
# Settings for TLS
tls:
  # if the server is using self-signed certificate, this need to be false. If true, you have to use CA signed certificate
  # or load truststore that contains the self-signed cretificate.
  verifyHostname: true
  # trust store contains certifictes that server needs. Enable if tls is used.
  loadTrustStore: true
  # trust store location can be specified here or system properties javax.net.ssl.trustStore and password javax.net.ssl.trustStorePassword
  trustStore: tls/client.truststore
  # key store contains client key and it should be loaded if two-way ssl is uesed.
  loadKeyStore: false
  # key store location
  keyStore: tls/client.keystore
# settings for OAuth2 server communication
oauth:
  # OAuth 2.0 token endpoint configuration
  token:
    # The scope token will be renewed automatically 1 minutes before expiry
    tokenRenewBeforeExpired: 60000
    # if scope token is expired, we need short delay so that we can retry faster.
    expiredRefreshRetryDelay: 2000
    # if scope token is not expired but in renew windown, we need slow retry delay.
    earlyRefreshRetryDelay: 4000
    # token server url. The default port number for token service is 6882.
    server_url: http://localhost:6882
    # token service unique id for OAuth 2.0 provider
    serviceId: com.networknt.oauth2-token-1.0.0
    # the following section defines uri and parameters for authorization code grant type
    authorization_code:
      # token endpoint for authorization code grant
      uri: "/oauth2/token"
      # client_id for authorization code grant flow. client_secret is in secret.yml
      client_id: 3798d583-275c-47d7-bf46-a3c436846336
      # the web server uri that will receive the redirected authorization code
      redirect_uri: https://localhost:8080/authorization_code
      # optional scope, default scope in the client registration will be used if not defined.
      scope:
      - customer.r
      - customer.w
    # the following section defines uri and parameters for client credentials grant type
    client_credentials:
      # token endpoint for client credentials grant
      uri: "/oauth2/token"
      # client_id for client credentials grant flow. client_secret is in secret.yml
      client_id: 6e9d1db3-2feb-4c1f-a5ad-9e93ae8ca59d
      # optional scope, default scope in the client registration will be used if not defined.
      scope:
      - account.r
      - account.w
  # light-oauth2 key distribution endpoint configuration
  key:
    # key distribution server url
    server_url: http://localhost:6886
    # the unique service id for key distribution service
    serviceId: com.networknt.oauth2-key-1.0.0
    # the path for the key distribution endpoint
    uri: "/oauth2/key"
    # client_id used to access key distribution service. It can be the same client_id with token service or not.
    client_id: 6e9d1db3-2feb-4c1f-a5ad-9e93ae8ca59d

``` 

And here is an example of secret.yaml with client secrets.

```yaml
# This file contains all the secrets for the server and client in order to manage and
# secure all of them in the same place. In Kubernetes, this file will be mapped to
# Secrets and all other config files will be mapped to mapConfig

---

# Sever section

# Key store password, the path of keystore is defined in server.yml
serverKeystorePass: password

# Key password, the key is in keystore
serverKeyPass: password

# Trust store password, the path of truststore is defined in server.yml
serverTruststorePass: password


# Client section

# Key store password, the path of keystore is defined in server.yml
clientKeystorePass: password

# Key password, the key is in keystore
clientKeyPass: password

# Trust store password, the path of truststore is defined in server.yml
clientTruststorePass: password

# Authorization code client secret for OAuth2 server
authorizationCodeClientSecret: f6h1FTI8Q3-7UScPZDzfXA

# Client credentials client secret for OAuth2 server
clientCredentialsClientSecret: f6h1FTI8Q3-7UScPZDzfXA

# Public key certificate retrieval client secret
keyClientSecret: f6h1FTI8Q3-7UScPZDzfXA

```
