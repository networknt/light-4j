---
date: 2016-10-07T22:02:13-04:00
title: Middleware Handlers
---

# Introduction

light-java is a Java API framework based on undertow http core that supports
swagger code generation and runtime request validation.

It contains the following components:

* [swagger-meta](https://networknt.github.io/light-java/middleware/swagger-meta/) is a 
middleware that load swagger at runtime and parse it based on the request uri and method and 
attached the swagger data related to the current endpoint into the exchange for subsequent 
handlers to use.

* [swagger-security](https://networknt.github.io/light-java/middleware/swagger-security/) 
Oauth2 JWT token verification distributed in every microservice. Also, there is an OAuth2 
server based on light-java released [here](https://github.com/networknt/light-oauth2)

* [swagger-validator](https://networknt.github.io/light-java/middleware/swagger-validator/) 
validates request based on the swagger.json for uri parameters, query parameters and body 
which is based on [json-schema-validator](https://github.com/networknt/json-schema-validator)

* [audit](https://networknt.github.io/light-java/middleware/audit/) logs most important info 
about request and response into audit.log in JSON format with config file that controls which
fieds to be logged.

* [body](https://networknt.github.io/light-java/middleware/body/) is a body parser middleware 
that is responsible for parsing the content of the request based on Content-Type in the 
request header. 

* [exception](https://networknt.github.io/light-java/middleware/exception/) is a generic 
exception handler that handles runtime exception, ApiException and other checked exception 
if they are not handled properly in the handler chain.

* [metrics](https://networknt.github.io/light-java/middleware/metrics/) is a module that collects
API runtime info based on clientId and API name. The metrics info is sent to InfluxDB and 
accessible from Grafana Dashboard.

* [sanitizer](https://networknt.github.io/light-java/middleware/sanitizer/) is a 
middleware that address cross site scripting concerns. It encodes header and body based on 
configuration.

* [correlation](https://networknt.github.io/light-java/middleware/correlation/) generates
a UUID in the first API/service and pass it to all other APIs/services in the call tree for
tracking purpose.

* [traceability](https://networknt.github.io/light-java/middleware/traceability/) is an
id passed in from client and will be unique with an application context. The id will be passed
into the backend and return to the consumer for transaction tracing. 

* [cors](https://networknt.github.io/light-java/middleware/cors/) is a module handles 
Cross-Origin Resource Sharing (CORS) pre-flight OPTIONS to support single page applications 
from another domain to access APIs/services.
 
* [dump](https://networknt.github.io/light-java/middleware/dump/) is a full request/response 
log handler to dump everything regarding to request and response into log file for developers. 

* [limit](https://networknt.github.io/light-java/middleware/limit/) is a rate limiting handler 
to limit number of concurrent requests on the server. Once the limit is reached, subsequent 
requests will be queued for later execution. The size of the queue is configurable. 

