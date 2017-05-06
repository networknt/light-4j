---
date: 2016-10-23T10:35:04-04:00
title: sanitizer
---

# Introduction

This is a middleware that addresses cross site scripting concerns. It encodes the header
and body according to the configuration. As body encoding depends on
[Body](https://networknt.github.io/light-4j/middleware/body/) middleware, it has to be
plugged into the request/response chain after Body.

# Configuration

Here is the default configuration sanitizer.yml

```
description: Sanitize request for cross site scripting during runtime
enabled: true
sanitizeBody: true
sanitizeHeader: true
```

If enabled is false, this middleware won't be loaded during server startup. 
sanitizeBody and sanitizeHeader control if body and header need to be sanitized or both.

# Query Parameters

In other platforms especially JEE containers, query parameters need to be sanitized as well.
However, I have found that Undertow does sanitize special characters in query parameters. This
is why this handler doesn't do anything about query parameters.

# Encode Library

The library used for cross site scripting sanitization is from https://www.owasp.org/index.php/Cross-site_Scripting_(XSS)
and the library can be found at https://github.com/OWASP/owasp-java-encoder

# Encode Level

The encode level we are using for both header and body is "forJavaScriptSource". This gives us
certain level of confident and it won't mess up header and body in most the case.

