---
date: 2016-10-23T13:22:33-04:00
title: Other Component
---


light-java is a Java API framework based on undertow http core that supports
swagger code generation and runtime request validation and security verification.

It contains the following components:

* [server](https://networknt.github.io/light-java/other/server/) - 
a framework on top of undertow http core that support plugins to performance 
different middleware handling.

* [config](https://networknt.github.io/light-java/other/config/) - 
A module that supports externalized configuration for standalone application and 
docker container.

* [utility](https://networknt.github.io/light-java/other/utility/) - 
utility classes that are shared between modules.

* [client](https://networknt.github.io/light-java/other/client/) - 
wrapper of apache HttpClient and HttpAsyncClient. support automatically cache and 
renew client credentials jwt token

* [info](https://networknt.github.io/light-java/other/info/) - 
a handler that injects an endpoint /server/info that can out all plugged in component 
on the server and configuration of each component.

* [mask](https://networknt.github.io/light-java/other/mask/) - 
used to mask sensitive info before logging to audit.log or server.log

* [status](https://networknt.github.io/light-java/other/status/) - 
used to model error http response and assist production monitoring with unique error 
code.

* [security](https://networknt.github.io/light-java/other/status/) - 
used by swagger-security currently but these utilities and helpers can be used by 
other security handlers.

* [swagger-codegen](https://github.com/networknt/swagger-codegen) - 
a code generator that generates the routing handlers and running API application 
based on swagger specification.

* [balance](https://networknt.github.io/light-java/other/balance/) -
A load balance module that is used by cluster module with service discovery module.

* [cluster](https://networknt.github.io/light-java/other/cluster/) -
A module caches discovered services and calling load balance module for load balancing.

* [consul](https://networknt.github.io/light-java/other/consul/) - 
A module manages communication with Consul server for registry and discovery.

* [handler](https://networknt.github.io/light-java/other/handler/) -
A module defines middleware handler interface for all middleware components.

* [Health](https://networknt.github.io/light-java/other/health/) -
A health check module that can be called by API portal to determine if the service is healthy.

* [registry](https://networknt.github.io/light-java/other/registry/) -
An interface definition and generic direct registry implementation for service
registry and discovery.

* [service](https://networknt.github.io/light-java/other/service/) -
A light weight dependency injection framework for testing and startup hooks.
 
* [switcher](https://networknt.github.io/light-java/other/switcher/) - 
A switcher that turn things on an off based on certain conditions.

* [zookeeper](https://networknt.github.io/light-java/other/zookeeper/) -
A module manages communication with ZooKeeper server for registry and discovery.


