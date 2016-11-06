---
date: 2016-10-23T12:26:20-04:00
title: Client
---
#

Client is used to call APIs from the following sources:

1. Web Server
2. Standalone Application/Mobile Application
3. API/Service

It provides method to get authorization token and automatically gets client
credentials token for scopes in API to API calls. It also helps to pass correlationId
and traceabilityId to the next service. 

# Type of client

## Sync Client
This is a wrapper of Apache HttpClient with built-in connection pools and TLS
support. It can be used to call another API/Service in the request context.

## Async Client
This is a wrapper of Apache HttpAsyncClient with built-in connection pools and
TLS support. It should be used if multiple APIs will be called in the request
context.


# Client Credentials token renew and cache
The renew of token happens behind the scene and it supports circuit breaker
if OAuth2 server is down or busy. It renews the token pro-actively before the
current one is expired and lets all requests go with the current token. It only
block other request if the current request is trying to renew an expired token.
When token renewal in this case fails, all request will be rejected with timeout
and subsequent requests the same until a grace period is passed so that the
renew process is start again. 

# Examples

### get sync client

```
    CloseableHttpClient client = Client.getInstance().getSyncClient();

```

### get async client

```
    CloseableHttpAsyncClient client = Client.getInstance().getAsyncClient();

```

### call from web server

To set the header with authorization code JWT token.

```
    public void addAuthToken(HttpRequest request, String token) 

```

To set the header with authorization code JWT token and traceabilityId.

```
    public void addAuthTokenTrace(HttpRequest request, String token, String traceabilityId) 
```

### call from standalone app/mobile app

To set the header with client credentials JWT token.
```
    public void addCcToken(HttpRequest request) throws ClientException, ApiException 
```

To set the header with client credentials JWT token and traceabilityId.

```
    public void addCcTokenTrace(HttpRequest request, String traceabilityId) throws ClientException, ApiException 

```

### call from API/service

To pass in exchange.

```
    public void propagateHeaders(HttpRequest request, final HttpServerExchange exchange) throws ClientException, ApiException 

```

To pass in variables individually.

```
    public void populateHeader(HttpRequest request, String authToken, String correlationId, String traceabilityId) throws ClientException, ApiException 

```
