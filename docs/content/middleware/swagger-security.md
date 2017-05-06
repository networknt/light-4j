---
date: 2016-10-12T19:07:43-04:00
title: Swagger Security
---

This handler is part of the [light-rest-4j](https://github.com/networknt/light-rest-4j)
which is built on top of light-4j but focused on RESTful API only.

It supports OAuth2 with JWT token distributed verification and can be extended to 
other authentication and authorization approaches. 

# JwtVerifyHandler

This is the handler that is injected during server start up if security.json 
enableVerifyJwt is true. It does further scope verification if enableVerifyScope 
is true against swagger specification.

# Distributed JWT verification

Unlike simple web token, the resource server has to contact Authorization server 
to validate the bearer token. JWT can be verified by resource server as long as 
the token signing certificate is available at resource server. 




