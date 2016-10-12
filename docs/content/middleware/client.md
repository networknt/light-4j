---
date: 2016-10-12T19:59:08-04:00
title: Client
---

Client is used to call APIs from the following sources:

1. Web server
2. Standalone Application
3. API

It provides method to get authorization jwt token and automatically gets client 
credentials token for scopes in API to API calls. 

# Sync Client
This is a wrapper of Apache HttpClient with built-in connection pools and TLS 
support. It can be used to call another API in the request context. 

# Async Client
This is a wrapper of Apache HttpAsyncClient with built-in connection pools and 
TLS support. It should be used it multiple APIs will be called in the request 
context. 

# Client Credentials token renew
The renew of token happens behind the scene and it supports circuit breaker 
is OAuth2 server is down or busy. It renew the token pro-actively before the 
current one is expired and let all requests go with the current token. It only 
block other request if the current request is trying to renew an expired token. 
When token renew in this case fails, all request will be rejected with timeout 
and subsequent requests the same until a grace period is passed so that the 
renew process is start again. 


