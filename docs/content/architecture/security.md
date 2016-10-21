---
date: 2016-10-20T14:34:09-04:00
title: security
---

# Introduction

Note: If this is the first time you hear about OAuth2 or you want to getting familiar with
the grant types we are using, please read this 
[article](https://github.com/networknt/undertow-oauth2/wiki/OAuth2-Introduction) first.

While designing microserivces, big monolithic application is breaking down to smaller
services that can be independently deployed or replaced. The final application will have
more http calls then a single application, how can we protect these calls between services?

To protect APIs, the answer is OAuth2 and most simple and popular solution will be 
simple web token as access token. The client authenticate itself on OAuth2 server and OAuth2
server issue
a simple web token (a UUID in most of the cases), then the client send the request to API
server with access token in the Authorization header. Once API server receives the request,
it has to send the access token to OAuth2 server to verify if this is valid token and if
this token is allowed to access this API. As you can see there must be a database lookup on
OAuth2 server to do that. Distributed cache help a lot but there is still a network call and
lookup for every single request. OAuth2 server eventually becomes a bottleneck and a single 
point of failure.

Years ago, when JWT draft spec. was out, I came up with the idea to do the 
distributed security verification with JWT to replace Simple Web Token for one of the big
banks in Canada. At that time, there is nobody using JWT this way and the bank sent the design to 
Paul Madson and John Bradley who are the Authors of OAuth2 and JWT specifications and got
their endorsement to use JWT this way.

Here is the diagram of distributed JWT verification for microservices.

![ms_distributed_jwt](/images/ms_distributed_jwt.png)




