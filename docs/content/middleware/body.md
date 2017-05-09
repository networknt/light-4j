---
date: 2016-10-12T18:57:17-04:00
title: Body Parser
---

Body parser is a middleware handler designed for [light-rest-4j](https://github.com/networknt/light-rest-4j) 
only. It will parse the body to a list or map depending on the first character of the 
body content if application/json is the content-type in the HTTP header for POST, PUT 
and PATCH HTTP methods. After the body is parsed, it will be attached to the exchange 
so that subsequent handlers can use it directly. In the future, other content-type 
might be supported if needed.

# Introduction

In order for this handler to work, the content-type in header must be started with 
"application/json". If the content type is correct, it will parse it to List or Map 
and put it into REQUEST_BODY exchange attachment.

If content type is missing or if it is not started as "application/json", the body
won't be parsed and this handler will just call next handler in the chain. 

[Sanitizer](https://networknt.github.io/light-4j/middleware/sanitizer/)
and [Swagger Validator](https://networknt.github.io/light-4j/middleware/swagger-validator/)
depend on this middleware.

# Configuration

Here is an example of configuration body.yml which has a flag to enable or disable it.

```
# Enable body parse flag
enabled: true
```
# Example

Here is the code example that get the boby object(Map or List) from exchange.

```
                    Object body = exchange.getAttachment(BodyHandler.REQUEST_BODY);
                    if(body == null) {
                        exchange.getResponseSender().send("nobody");
                    } else {
                        if(body instanceof List) {
                            exchange.getResponseSender().send("list");
                        } else {
                            exchange.getResponseSender().send("map");
                        }
                    }

```

