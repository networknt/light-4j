---
date: 2016-10-12T19:07:43-04:00
title: Security
---

The current framework supports OAuth2 with JWT token but can be extended to 
other authentication and authorization approaches. 

# light-oauth2 server

By default, the framework contains two pairs of public key certificates issued 
by our own oauth2 server which can be installed from docker. For more info, 
please refer to https://github.com/networknt/light-oauth2

# Kid

Since services are deployed in the cloud without static IP, the traditional
push certificates to each service is not working anymore. In this framework
each service will pull certificate from OAuth2 server key service by a kid
from JWT token if the key doesn't exist locally.  

# JwtMockHandler

This is a testing OAuth2 endpoints provider and it can be injected into the handler 
chain for unit testing so that it won't depend on an instance of 
light-oauth2. 

# Long lived token

To make integration test easier, a long lived token is provided by the oauth2 
server and it can be found at https://github.com/networknt/light-oauth2

