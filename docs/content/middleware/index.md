---
date: 2016-10-07T22:02:13-04:00
title: Middleware Handlers
---

# Introduction

Light-4J is a Java API framework based on undertow http core which supports request and 
response manipulations in HttpServerExchange. Unlike servlet filter, it is very easy to add
middleware handlers in the request and response chain to address all the cross-cutting
concerns.  

There are two types of middleware handlers: technical and contextual. All the middleware 
handlers provided by light-4j are technical as we don't care about each individual service 
but apply the handlers blindly. For example, we have body parse that parse the body into either 
List or Map if content-type is application/json. We cannot parse the body into a POJO as we have 
no knowledge about which POJO class to use. In order to parse it into POJO you can implement 
your business logic with multiple handlers, one parse the request body into POJO and attached 
to the exchange and the next handler does the real business logic. You can also inject other 
handlers to do fine-grained authorization and request and response filtering and transformation, 


It contains the following components:

* [audit](https://networknt.github.io/light-4j/middleware/audit/) logs most important info 
about request and response into audit.log in JSON format with config file that controls which
fields to be logged.

* [body](https://networknt.github.io/light-4j/middleware/body/) is a body parser middleware 
that is responsible for parsing the content of the request based on Content-Type in the 
request header. 

* [exception](https://networknt.github.io/light-4j/middleware/exception/) is a generic 
exception handler that handles runtime exception, ApiException and other checked exception 
if they are not handled properly in the handler chain.

* [metrics](https://networknt.github.io/light-4j/middleware/metrics/) is a module that collects
API runtime info based on clientId and API name. The metrics info is sent to InfluxDB and 
accessible from Grafana Dashboard.

* [sanitizer](https://networknt.github.io/light-4j/middleware/sanitizer/) is a 
middleware that address cross site scripting concerns. It encodes header and body based on 
configuration.

* [correlation](https://networknt.github.io/light-4j/middleware/correlation/) generates
a UUID in the first API/service and pass it to all other APIs/services in the call tree for
tracking purpose.

* [traceability](https://networknt.github.io/light-4j/middleware/traceability/) is an
id passed in from client and will be unique with an application context. The id will be passed
into the backend and return to the consumer for transaction tracing. 

* [cors](https://networknt.github.io/light-4j/middleware/cors/) is a module handles 
Cross-Origin Resource Sharing (CORS) pre-flight OPTIONS to support single page applications 
from another domain to access APIs/services.
 
* [dump](https://networknt.github.io/light-4j/middleware/dump/) is a full request/response 
log handler to dump everything regarding to request and response into log file for developers. 

* [limit](https://networknt.github.io/light-4j/middleware/limit/) is a rate limiting handler 
to limit number of concurrent requests on the server. Once the limit is reached, subsequent 
requests will be queued for later execution. The size of the queue is configurable. 

