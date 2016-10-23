---
date: 2016-10-23T13:22:33-04:00
title: Other Component
---

# Introduction

light-java is a Java API framework based on undertow http core that supports
swagger code generation and runtime request validation and security verification.

It contains the following components:

* [server](https://networknt.github.io/light-java/middleware/server/) - a framework on top of undertow http core that support plugins to
performance different middleware handling.

* [config](https://networknt.github.io/light-java/middleware/config/) - A module
that supports externalized configuration for standalone application and docker
container.

* [utility](https://networknt.github.io/light-java/middleware/utility/) - utility classes that are shared between modules.

* [client](https://networknt.github.io/light-java/middleware/client/) - wrapper of apache HttpClient and HttpAsyncClient. support automatically
cache and renew client credentials jwt token

* [info](https://networknt.github.io/light-java/middleware/info/) - a handler that
injects an endpoint /server/info that can out all plugged in component on the
server and configuration of each component.

* [mask](https://networknt.github.io/light-java/middleware/mask/) - used to mask sensitive info before logging to audit.log or server.log

* [status](https://networknt.github.io/light-java/middleware/status/) - used to model error http response and assist production monitoring
with unique error code.

* [swagger-codegen](https://github.com/networknt/swagger-codegen) - a code generator
that generates the routing handlers and running API application based on swagger specification.
