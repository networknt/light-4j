---
date: 2017-09-21T21:20:27-04:00
title: Why check token expiration in client
---

In web service architecture, normally people handle JWT token expiration reactively. Here
is the flow. 

* Client sent request with a JWT token in header.
* Service receives the request and verify if the JWT token expires
* If expired, then return 401 - token expired
* When client receives this error and body, it will go to the OAuth 2.0 provider to renew a new token
* Resend the request with the new token.

Note that if token is not expired then go to the next step. 


The above flow doesn't work with microservices architecture as it will cause data consistency
issue if the token being used is about to expire. 

For example, the client is calling two services in sequence. The first service verify the token
and it is not expired and the transaction went though. However, when the second service receives
the token, it is already expired and return an error message to the client. In this scenario, the
client can still renew the token and resend the second request with the new token. 

Let's take a look at the next scenario. The client is calling first service and the first service
calls the second service. what if the first service passed but the second service got a token
expired error? If we still follow the above flow, then we need the client to renew the token
and retry first service again then first service calls to second service. This requires the first
service must be idempotent with is much more complicated to handle.

To avoid these complicated scenario, in light-4j framework, we check the token expiration pro-actively
in the [client](https://networknt.github.io/light-4j/other/client/) module and renew a new token before 
it is about to expire. The default configuration is 1 minutes before token expiration a separate thread 
will contact [light-oauth2](https://networknt.github.io/light-oauth2/) token service to renew the token. 

