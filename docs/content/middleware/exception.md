---
date: 2016-10-12T19:03:24-04:00
title: exception
---

If any handler throws an exception within the handler chain, that exception 
will bubble up to the undertow server and eventually a 500 response will be 
sent to the consumer. In order to change the behaviour, an exception handler 
is provided to handle ApiException and other uncaught exceptions.

# Runtime Exception

Any runtime exception will be captured and return a standard 500 error with 
error code ERR10010.

# Uncaught Exception

Any checked exception that is not handled by handlers in the handler chain 
is captured and return a 400 error with error code ERR10011

# ApiException

ApiException has a status object and it will return to consume the data defined 
in the status object.


# Logging

Exception handler will log the exception with stacktrace.