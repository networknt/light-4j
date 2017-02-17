---
date: 2016-10-07T22:02:13-04:00
title: Middleware Handlers
---

# Introduction

light-java is a Java API framework based on undertow http core that supports
swagger code generation and runtime request validation.

It contains the following components:

* [swagger-security](https://networknt.github.io/light-java/middleware/swagger-security/) - 
Oauth2 jwt token verification distributed in every microservice. Also, there is an OAuth2 
server based on light-java released [here](https://github.com/networknt/light-oauth2)

* [swagger-validator](https://networknt.github.io/light-java/middleware/swagger-validator/) - 
Validate request based on the swagger.json for uri parameters, query parameters and body 
which is based on [json-schema-validator](https://github.com/networknt/json-schema-validator)

* [audit](https://networknt.github.io/light-java/middleware/audit/) - Log most important info 
about request and response into audit.log in JSON format with config file that controls which
fieds to be logged.

* [body](https://networknt.github.io/light-java/middleware/body/) - A body parser middleware 
that is responsible for parsing the content of the request based on Content-Type in the 
request header. 

* [exception](https://networknt.github.io/light-java/middleware/exception/) - A generic 
exception handler that handles runtime exception, ApiException and other checked exception 
if they are not handled properly in the handler chain.

* [metrics](https://networknt.github.io/light-java/middleware/metrics/) - A module that collect
API runtime info based on clientId and API name. The metrics info is sent to InfluxDB and 
accessible from Grafana Dashboard.

* [swagger-meta](https://networknt.github.io/light-java/middleware/swagger-meta/) - This is a 
middleware that load swagger at runtime and parse it based on the request uri and method and 
attached the swagger data related to the current endpoint into the exchange for subsequent 
handlers to use.

* [sanitizer](https://networknt.github.io/light-java/middleware/sanitizer/) - This is a 
middleware that address cross site scripting concerns. It encodes header and body based on 
configuration.

* [correlation](https://networknt.github.io/light-java/middleware/correlation/) - Generate
a UUID in the first API/service and pass it to all other APIs/services in the call tree for
tracking purpose.

* [traceability](https://networknt.github.io/light-java/middleware/traceability/) - It is an
Id passed in from client and will be unique with an application context. The Id will be passed
into the backend and return to the consumer for transaction tracing. 

* [cors](https://networknt.github.io/light-java/middleware/cors/) - The Cross-Origin Resource 
Sharing (CORS) handler supports single page applications from another domain to access
 APIs/services.
 
* [dump](https://networknt.github.io/light-java/middleware/dump/) - 
A full request/response log handler to dump everything regarding to request and response 
into log file for developers. 

* [limit](https://networknt.github.io/light-java/middleware/limit/) - 
A rate limiting handler to limit number of concurrent requests on the server.
Once the limit is reached, subsequent requests will be queued for later execution. The
size of the queue is configurable. 

