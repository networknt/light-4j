---
date: 2017-02-06T21:33:25-05:00
title: Handler
---

This module defines an interface that all middleware handlers must be implemented
in order to be loaded during server startup.

```
public interface MiddlewareHandler extends HttpHandler {

    HttpHandler getNext();

    MiddlewareHandler setNext(final HttpHandler next);

    boolean isEnabled();

    void register();

}

```

