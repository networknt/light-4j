---
date: 2016-10-07T22:02:13-04:00
title: Middleware Handlers
---

# Introduction

light-java is a Java API framework based on undertow http core that supports
swagger code generation and runtime request validation.

It contains the following components:

* [server](https://networknt.github.io/light-java/middleware/server/) - a framework on top of undertow http core that support plugins to
performance different middleware handling.

* [security](https://networknt.github.io/light-java/middleware/security/) - oauth2 jwt token verification and mock token generation. Also,
there is an testing OAuth2 server released [here](https://github.com/networknt/light-oauth2)

* [config](https://networknt.github.io/light-java/middleware/config/) - A module
that supports externalized configuration for standalone application and docker 
container.

* [utility](https://networknt.github.io/light-java/middleware/utility/) - utility classes that are shared between modules.

* [client](https://networknt.github.io/light-java/middleware/client/) - wrapper of apache HttpClient and HttpAsyncClient. support automatically
cache and renew client credentials jwt token

* [validator](https://networknt.github.io/light-java/middleware/validator/) - validate request based on the swagger.json for uri parameters,
query parameters and body which is based on [json-schema-validator](https://github.com/networknt/json-schema-validator)

* [audit](https://networknt.github.io/light-java/middleware/audit/) - dump most important info about request and response into audit.log in
JSON format. Also, there is a full audit handler to dump everything regarding to 
request and response.

* [info](https://networknt.github.io/light-java/middleware/info/) - a handler that
injects an endpoint /server/info that can out all plugged in component on the 
server and configuration of each component.

* [mask](https://networknt.github.io/light-java/middleware/mask/) - used to mask sensitive info before logging to audit.log or server.log

* [status](https://networknt.github.io/light-java/middleware/status/) - used to model error http response and assist production monitoring
with unique error code.

* [body](https://networknt.github.io/light-java/middleware/body/) A body parser middleware that is
responsible for parsing the content of the request based on Content-Type in the request header. 

* [exception](https://networknt.github.io/light-java/middleware/exception/) A generic exception
handler that handles runtime exception, ApiException and other checked exception if they are not handled
properly in the handler chain.

* [metrics](https://networknt.github.io/light-java/middleware/metrics/) A module that collect
API runtime info based on clientId and API name. The metrics info is sent to InfluxDB and accessible
from Grafana Dashboard.

* [swagger](https://networknt.github.io/light-java/middleware/swagger/) This is a middleware
that load swagger at runtime and parse it based on the request uri and method and attached the 
swagger data related to the current endpoint into the exchange for subsequent handlers to use.

* [swagger-codegen](https://github.com/networknt/swagger-codegen) - a code generator
that generates the routing handlers and running API application based on swagger specification. 
