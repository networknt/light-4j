---
date: 2016-10-07T22:02:13-04:00
title: Middleware Handlers
---

# Introduction

undertow-server is a Java API framework based on undertow http core that supports 
swagger code generation and runtime request validation.

It contains the following components:

* server - a framework on top of undertow http core that support plugins to 
performance different middleware handling.

* security - oauth2 jwt token verification and mock token generation. Also, 
there is an testing OAuth2 server released [here](https://github.com/networknt/undertow-server-oauth2)

* [config](https://github.com/networknt/undertow-server/wiki/Config) - A module 
that supports externalized configuration for standalone application and docker 
container.

* utility - utility classes that are shared between modules.

* client - wrapper of apache HttpClient and HttpAsyncClient. support automatically 
cache and renew client credentials jwt token

* validator - validate request based on the swagger.json for uri parameters, 
query parameters and body which is based on [json-schema-validator](https://github.com/networknt/json-schema-validator)

* audit - dump most important info about request and response into audit.log in 
JSON format. Also, there is a full audit handler to dump everything regarding to 
request and response.

* [info](https://github.com/networknt/undertow-server/wiki/Info) - a handler that 
injects an endpoint /server/info that can out all plugged in component on the 
server and configuration of each component.

* mask - used to mask sensitive info before logging to audit.log or server.log

* status - used to model error http response and assist production monitoring 
with unique error code.

* swagger-codegen - a [generator](https://github.com/networknt/swagger-codegen) 
that generates the routing handlers and running API application with swagger spec. 

# Getting started