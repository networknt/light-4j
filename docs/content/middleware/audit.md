---
date: 2016-10-12T18:48:58-04:00
title: Audit
---

There are two built-in audit handlers that write logs into audit.log that setup 
in the logback appender. 

# SimpleAuditHandler
Only logs several fields from request header and the fields are configurable. 
Optional, it can log response status and response time.

# FullAuditHandler
Dump every thing from request and response. This is mainly a development tool 
and may be used on production for some of the APIs without performance concerns.

# Customized Handler
For some users that need special audit logic or other channel to redirect the audit
to, they can create their own audit handler and replace the default audit handler in
/src/main/resources/META-INF/services/com.networknt.handler.MiddlewareHandler

Before updating this file, please read this [document]()
