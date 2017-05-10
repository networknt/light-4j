---
date: 2016-10-12T19:03:24-04:00
title: Exception Handler
---

This is a middleware handler that should be put in the beginning of request/response
chain. It wraps the entire chain so that any un-handled exceptions will finally reach
here and to be handled gracefully. It is encouraged to handle exceptions in business
handler because the context is clear and the exchange will be terminated at the right
place.

This handler is plugged in by default from light-codegen and it should be enabled on
production as the last defence line. It also dispatch the request to worker thread
pool from IO thread pool. Only turn this off if you understand the impact and have a
very good reason to do so.

If any handler throws an exception within the handler chain, that exception will 
bubble up to the undertow server and eventually a 500 response will be sent to the 
consumer. In order to change the behaviour, this exception handler is provided to 
handle ApiException and other uncaught exceptions.

## Runtime Exception

Any runtime exception will be captured and return a standard 500 error with error 
code ERR10010.

## Uncaught Exception

Any checked exception that is not handled by handlers in the handler chain is captured 
and return a 400 error with error code ERR10011

## ApiException

ApiException has a status object and it will return to consume the data defined in the 
status object.


## Logging

Exception handler will log the exception with stacktrace.

## Undertow Logging

As Undertow is using JBoss logger and we are using slf4j with logback. The Undertow logs
are not shown up in our log files. In order to capture exception thrown in Undertow, there
are some special handling need to be done. 

* In this exception handler, we need to dispatch the request from IO thread pool to worker
thread pool so that all exceptions thrown from Undertow will be handled by handlers instead
of Connectors class. 
* In Server.java we have setup the property 
'System.setProperty("org.jboss.logging.provider", "slf4j");'

## Exceptions

There are several exception classes in the framework and here is the list.

* ApiException - checked exception to wrap Status instance for formatted response.
* FrameworkException - runtime exception to wrap Status instance for formatted response.
* ConfigException - used by Config module to indicate incorrect config file format.
* ClientException - used by Client module to handle exception communicating other services.
* ExpiredTokenException - used by JWT verifier to indicate token expired.

