---
date: 2016-10-12T18:57:17-04:00
title: Body Parser
---

# Introduction

Body is an HttpHandler to parse the body according to the content type int the 
request header and attach the parsed result into the exchange so that subsequent 
handlers will use it directly. 

Only POST, PUT and PATCH methods can have body and currently only JSON is supported
as only light-java-rest is built on top of light-java at the moment. In the future,
other content type will be supported.

It needs content type header to be started with "application/json". If the content 
type is correct, it will parse it to List or Map and put it into REQUEST_BODY 
exchange attachment.

If content type is missing or if it is not started as "application/json", the body
won't be parsed and this handler will just call next handler in the chain. 

[Sanitizer](https://networknt.github.io/light-java/middleware/sanitizer/)
and [Swagger Validator](https://networknt.github.io/light-java/middleware/swagger-validator/)
depend on this middleware.

# Configuration

Here is an example of configuration which has a flag to enable or disable it.

```
{
  "description": "Body parser handler",
  "enabled": true
}

```
