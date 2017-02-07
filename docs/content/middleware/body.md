---
date: 2016-10-12T18:57:17-04:00
title: Body Parser
---

# Introduction

Body is an HttpHandler to parse the body according to the content type int the 
request header and attach the parsed result into the exchange so that subsequent 
handlers will use it directly. 

The current implementation only supports JSON and needs content type header to be
started with "application/json". If the content type is correct, it will parse it
to List or Map and put it into REQUEST_BODY exchange attachment.

If content type is missing or if it is not started as "application/json", the body
won't be parsed.

[Sanitizer](https://networknt.github.io/light-java/middleware/sanitizer/)
and [Swagger Validator](https://networknt.github.io/light-java/middleware/swagger-validator/)
depend on this middleware.
