---
date: 2017-02-15T09:26:58-05:00
title: Cross-Origin Resource Sharing
---

For some of the APIs/services, the endpoints will be accessed from a Single Page 
Application(React/Vue/Angular) served from another domain. In this case, the API
server needs to handle the pre-flight OPTIONS request to enable CORS. 

As CORS only used in above scenario, the handler is not wired in by default in
swagger-codegen. 

If you want to limit only several domains for CORS, you also need to create cors.yml
in config folder.

And here is an example of cors.yml

```
description: Cors Http Handler
enabled: true
allowedOrigins:
- http://localhost
allowedMethods:
- GET
- POST
```

To enable CORS support, you need to add cors module in pom.xml, you need to update 
pom.xml to add a dependency.

```
        <dependency>
            <groupId>com.networknt</groupId>
            <artifactId>cors</artifactId>
            <version>${version.light-4j}</version>
        </dependency>

```

Update com.networknt.handler.MiddlewareHandler in src/main/resources/META-INF/services
folder to plug cors handler into the middleware chain. Note that cors handler is in front
of swagger so that pre-flight OPTIONS request will be returned before swagger validation
is done as OPTIONS methods are not defined in swagger specifitions. 

```
# This file is generated and should not be changed unless you want to plug in more handlers into the handler chain
# for cross cutting concerns. In most cases, you should replace some of the default handlers with your own implementation
# Please note: the sequence of these handlers are very important.

#Validator Validate request based on swagger specification (depending on Swagger and Body)
com.networknt.validator.ValidatorHandler
#Sanitizer Encode cross site scripting
com.networknt.sanitizer.SanitizerHandler
#SimpleAudit Log important info about the request into audit log
com.networknt.audit.AuditHandler
#Body Parse body based on content type in the header.
com.networknt.body.BodyHandler
#Security JWT token verification and scope verification (depending on SwaggerHandler)
com.networknt.security.JwtVerifyHandler
#Swagger Parsing swagger specification based on request uri and method.
com.networknt.swagger.SwaggerHandler
#Cors handler to handler post/put pre-flight
com.networknt.cors.CorsHttpHandler
#Correlation Create correlationId if it doesn't exist in the request header and put it into the request header
com.networknt.correlation.CorrelationHandler
#Traceability Put traceabilityId into response header from request header if it exists
com.networknt.traceability.TraceabilityHandler
#Metrics In order to calculate response time accurately, this needs to be the second.
com.networknt.metrics.MetricsHandler
#Exception Global exception handler that needs to be called first.
com.networknt.exception.ExceptionHandler
```


Now let's start the server
```
cd ~/networknt/light-example-4j/cors
mvn clean install exec:exec
```

Test the pre-flight OPTIONS.

```
curl -H "Origin: http://example.com" -H "Access-Control-Request-Method: POST"  -H "Access-Control-Request-Headers: X-Requested-With"  -X OPTIONS --verbose http://localhost:8080/v1/postData
```

And the result
```
*   Trying 127.0.0.1...
* Connected to localhost (127.0.0.1) port 8080 (#0)
> OPTIONS /v1/postData HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.43.0
> Accept: */*
> Origin: http://example.com
> Access-Control-Request-Method: POST
> Access-Control-Request-Headers: X-Requested-With
> 
< HTTP/1.1 200 OK
< Access-Control-Allow-Headers: X-Requested-With
< Server: Light
< Access-Control-Allow-Credentials: true
< Content-Length: 0
< Access-Control-Allow-Methods: POST
< Access-Control-Max-Age: 3600
< Date: Wed, 15 Feb 2017 13:37:29 GMT
< 
* Connection #0 to host localhost left intact

```

The source code for the cors example can be found at

https://github.com/networknt/light-example-4j/tree/master/cors

