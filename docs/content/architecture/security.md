---
date: 2016-10-20T14:34:09-04:00
title: Security
---


Note: If this is the first time you hear about OAuth2 or you want to get familiar with
the grant types we are using, please read this 
[article](https://github.com/networknt/light-oauth2/wiki/OAuth2-Introduction) first.

Everyone’s excited about microservices, but actual implementation is sparse. Perhaps the
reason is that people are unclear on how these services talk to one another; especially 
tricky thing is access management throughout a sea of independent services.

While designing microserivces, big monolithic application is breaking down to smaller
services that can be independently deployed or replaced. The final application will have
more http calls than a single application, how can we protect these calls between services?

To protect APIs/services, the answer is OAuth2 and most simple and popular solution will be
simple web token as access token. The client authenticates itself on OAuth2 server and OAuth2
server issues
a simple web token (a UUID in most of the cases), then the client sends the request to API
server with access token in the Authorization header. Once API server receives the request,
it has to send the access token to OAuth2 server to verify if this is valid token and if
this token is allowed to access this API. As you can see there must be a database lookup on
OAuth2 server to do that. Distributed cache helps a lot but there is still a network call and
lookup for every single request. OAuth2 server eventually becomes a bottleneck and a single 
point of failure.

Years ago, when JWT draft specification was out, I came up with the idea to do the
distributed security verification with JWT to replace Simple Web Token for one of the big
banks in Canada. At that time, there was nobody using JWT this way and the bank sent the design to
Paul Madson and John Bradley who are the Authors of OAuth2 and JWT specifications and got
their endorsements to use JWT this way.

# Distributed JWT Verification

Here is the diagram of distributed JWT verification for microservices.

![ms_distributed_jwt](/images/ms_distributed_jwt.png)



Let's assume the following:

* Client1 is a web server and it has client1 as client_id.
* API A is a microservice and it has apia as client_id and it requires a.r scope to access.
* API B is a microserivce and it has apib as client_id and it requires b.r scope to access.
* API C is a microservice and it has apic as client_id and it requires c.r scope to access.
* API D is a microservice and it has apid as client_id and it requires d.r scope to access.

### User trigger the authentication

When user clicks the login button or accesses resource that is protected, he will be
redirect to OAuth2 server to authenticate himself. After input username and password, an
authorization code is redirected back to the browser. The client1 will handle the redirect
url and get the authorization code. By sending client1 client_id, client_secret and
authorization code from user to OAuth2 server, Client1 gets a JWT token with

user_id = user1

client_id = client1

scope = [a.r]

This token will be put into the Authorization header of the request to API A. When API A
receives the request, it verifies the JWT token with public key issued by OAuth2 server with
the security middleware in the framework. If the signature verification is successful, it
will verify the scope in the token against the swagger specification defined for the
endpoint Client1 is accessing. As a.r is required and it is in the JWT scope, it allows
the access.

### API A calls API B and API C

Now API A needs to call API B and API C to fulfill the request. As this is API to API call or
service to service call, there is no user id involved and Client Credentials flow will be
used here to get another JWT token to B and C. The original JWT token doesn't have the scopes
to access B and C as Client1 does not even care A is calling B and C. So here API A needs to
get a token associate with client_id apia which has proper scope to access API B and API C.

This token will have the following claims.

client_id = apia

scope = [b.r c.r]

As the original token has the original user_id, it is carried in the entire service to service
calls so that each service has a chance to do role based or user based authorization if it is
necessary. The new client_credentials token will be passed in the request header "X-Scope-Token".

### API B and API C tokens verification

The tokens verification on API B and API C are the same. So le't use API B as an example to
explain the verification process.

When API B receives the request, it first check the Authorization token to make sure it is valid. Then
if scope verification is enabled, it will check if 'X-Scope-Token' header exists. If yes, it will verify
it and match the scope with endpoint defined scope. If scope matching is failed, it will fall back
to Authorization token to see if it has the scope required. If none of the tokens has the scope required,
an error will be sent back to the caller.

### API B calls API D

The process is very similar like API A calls API B and API C. A client credentials token will be
retrieved by API B from OAuth2 server and it has the following claims.

client_id = apib

scope = [d.r]

### API D token verification

Similar like API B and API C tokens verification.


# Client Credentials / Scope Token Cache

As described above, for every API to API call, the caller must pass in a scope token in addition to
the original token. Unlike the original token which is associated with a user, the scope token is only
associated with a client (API / service) and it won't be expired after a period configured on OAuth2
server. So it is not necessary to get the new scope token for every API to API call. The token is
retrieved and cached in memory until it is about to be expired then a new token will be retrieved.

The entire token renew process is managed
by [Client](https://networknt.github.io/light-4j/other/client/) module provided in the light-4j
framework. This client module encapsulate a lot of features to help API to API calls.

# Authorization Token Cache

The original token normally will be cached in the web server session so that the subsequent calls
from the same user can use the cached token.

# Single Sign On

As the end user login is managed by OAuth2 server, there is a session established between user's
browser and OAuth2 server. The the user switch to another tab on his browser to access another
application, the login on OAuth2 is not necessary and a new authorization code will immediately
redirected back.


# Fine-Grained Authorization

Above authorization is based on the client_id and scope in the JWT token and it can be verified by 
JwtVerifierHandler without any context information regarding to a specific API endpoint. And it can
be applied blindly as a technical middleware handler at the framework level. For other fine-grained
authorizations, they must be applied based on the context of the service. 

### Role-Based 

Role-Based Authorization must be specific to one or a group of services with a set of pre-defined
roles. Normally the role can be embedded into the Id token and the mapping should be cached in the
handler.

### Attribute-Based
 
For example, only Account manager can access an account. Or employee based on one geo location can
access customer based on the same Geo location.

### Rule-Based

Teller can only transfer money less than $10,000.00 etc.


# Request and Response Filter

Sometimes, certain request fields need to be removed based on the clientId or other attributes so that
the rule business handler can be done in a consistent way. Also, for certain client or user, the
response might need to filter out some information before returning to the consume. 

# JWT public key certificate distribution

As we are using JWT token for distributed token verification on each service instance, we need to make
sure that the public key certificate from OAuth 2.0 provider can be distributed to each individual
service instance so that they can verify the token issued by the same OAuth 2.0 token service independently.

Here is the [document](https://networknt.github.io/light-4j/architecture/key-distribution/) that describes how key is distributed to the services during runtime.  
