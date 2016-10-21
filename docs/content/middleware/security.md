---
date: 2016-10-12T19:07:43-04:00
title: security
---

The current framework supports OAuth2 with JWT token but can be extended to 
other authentication and authorization approaches. 

# JwtVerifyHandler

This is the handler that is injected during server start up if security.json 
enableVerifyJwt is true. It does further scope verification if enableVerifyScope 
is true against swagger specification.

# Distributed JWT verification

Unlike simple web token, the resource server has to contact Authorization server 
to validate the bearer token. JWT can be verified by resource server as long as 
the token signing certificate is available at resource server. Due to security 
concerns, there should be two pair of private key and public key certificate 
available at any time in case the primary key is compromised. 

# light-oauth2 server

By default, the framework contains two pairs of public key certificates issued 
by our own oauth2 server which can be installed from docker. For more info, 
please refer to https://github.com/networknt/light-oauth2

# Kid

Since there are two certificates available, the kid in the header of the JWT 
token will decide which certificate will be used to verify the JWT signature. 

# JwtMockHandler

This is a testing OAuth2 endpoints provider and it can be injected into the handler 
chain for unit testing so that it won't depend on an instance of 
undertow-oauth2. 

# Long lived token

To make integration test easier, a long lived token is provided by the oauth2 
server and it can be found at https://github.com/networknt/undertow-oauth2





 