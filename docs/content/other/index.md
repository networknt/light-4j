---
date: 2016-10-23T13:22:33-04:00
title: Other Components
---


light-4j is a Java API framework based on undertow http core that supports
swagger/graphql/hybrid code generation and runtime request validation and 
security verification.

It contains the following components:

* [server](https://networknt.github.io/light-4j/other/server/) is
a framework on top of Undertow http core that support plugins to perform 
different middleware handlers. It is light-weight, fast and supports HTTP/2.

* [config](https://networknt.github.io/light-4j/other/config/) is a module that 
supports externalized yml/yaml/json configuration for standalone applications and 
docker containers managed by Kubernetes. Config files are managed by 
[light-config-server](https://github.com/networknt/light-config-server) and mapped
to Kubernetes ConfigMap and Secrets.

* [utility](https://networknt.github.io/light-4j/other/utility/) contains utility 
classes and static variables that are shared between modules.

* [client](https://networknt.github.io/light-4j/other/client/) is a wrapper of 
apache HttpClient and HttpAsyncClient. It supports automatically cache and 
renew client credentials JWT tokens and manages connection pooling. It is also
responsible for passing correlationId and traceabilityId to the next service.

* [info](https://networknt.github.io/light-4j/other/info/) is a handler that 
injects an endpoint /server/info to all server instances so that light-portal
can pull the info to certify all the enabled components and their configuration
at runtime. It also helps while debugging issues on the server.

* [mask](https://networknt.github.io/light-4j/other/mask/) is used to mask 
sensitive info before logging. 

* [status](https://networknt.github.io/light-4j/other/status/) is used to model 
error http response and assist production monitoring with unique error code.

* [security](https://networknt.github.io/light-4j/other/status/) is used by 
swagger-security and graphql-security currently but these utilities and helpers can 
be used by other security handlers for Role-Based or Attribute-Based Authorization.

* [light-codegen](https://github.com/networknt/light-codegen) is a code generator 
that generates the routing handlers and running API application based on swagger 
specification/GraphQL IDL/Hybrid schema. The generated code contains mock handler 
and test cases and it can be used for simple integration tests.

* [balance](https://networknt.github.io/light-4j/other/balance/) is a load balance 
module that is used by cluster module with service discovery module. It will be called
from client module and be part of client side discovery. 

* [cluster](https://networknt.github.io/light-4j/other/cluster/) ia a module caches 
discovered services and calling load balance module for load balancing. Part of client
side discovery.

* [consul](https://networknt.github.io/light-4j/other/consul/) is a module manages 
communication with Consul server for registry and discovery.

* [handler](https://networknt.github.io/light-4j/other/handler/) is a module defines 
middleware handler interface for all middleware components.

* [Health](https://networknt.github.io/light-4j/other/health/) is a health check module 
that can be called by API portal to determine if the service is healthy. It supports
cascade health check for databases or message queues.

* [registry](https://networknt.github.io/light-4j/other/registry/) ia an interface 
definition and generic direct registry implementation for service registry and discovery.

* [service](https://networknt.github.io/light-4j/other/service/) is a light weight 
dependency injection framework for testing and startup hooks.
 
* [switcher](https://networknt.github.io/light-4j/other/switcher/) is a switcher that 
turns things on and off based on certain conditions.

* [zookeeper](https://networknt.github.io/light-4j/other/zookeeper/) is a module manages 
communication with ZooKeeper server for service registry and discovery.

