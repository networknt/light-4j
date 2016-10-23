---
date: 2016-10-07T22:02:13-04:00
title: Middleware Handlers
---

# Introduction

light-java is a Java API framework based on undertow http core that supports
swagger code generation and runtime request validation.

It contains the following components:

* [security](https://networknt.github.io/light-java/middleware/security/) - oauth2 jwt token verification and mock token generation. Also,
there is an testing OAuth2 server released [here](https://github.com/networknt/light-oauth2)

* [validator](https://networknt.github.io/light-java/middleware/validator/) - validate request based on the swagger.json for uri parameters,
query parameters and body which is based on [json-schema-validator](https://github.com/networknt/json-schema-validator)

* [audit](https://networknt.github.io/light-java/middleware/audit/) - dump most important info about request and response into audit.log in
JSON format. Also, there is a full audit handler to dump everything regarding to 
request and response.

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

* [sanitizer](https://networknt.github.io/light-java/middleware/sanitizer/) This is a middleware
that address cross site scripting concerns. It encodes header and body based on configuration.