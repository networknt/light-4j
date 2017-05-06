---
date: 2016-03-08T21:07:13+01:00
title: Introduction
type: index
weight: 0
---

## Why this framework

### Fast and small memory footprint to lower production cost.

It is 44 times faster than the most popular microservices platform Spring Boot embedded 
Tomcat and use only 1/5 of memory. Here is the [benchmark](https://github.com/networknt/microservices-framework-benchmark) 
results compare with Spring Boot and other microservices frameworks.

### Provide an embedded gateway to address cross cutting concerns.
* Plugin architecture for startup/shutdown hooks and middleware components
* Distributed OAuth2 JWT security verification as part of the framework
* Request and response validation against OpenAPI specification at runtime
* Metrics collected in influxdb and viewed from Grafana Dashboard for both services and clients
* Global exception handling for runtime exception, api exception and other checked exceptions
* Mask sensitive data like credit card, sin number etc. before logging
* Sanitize cross site scripting for query parameters, request headers and body
* Audit to dump important info or entire request and response.
* Body parser to support different content types
* Standardized response code and messages from configuration file
* Externalized configuration for all modules for dockerized environment 
* CORS pre-flight handler for SPA (Augular or React) from another domain
* Rate limiting for services that exposed outside to the Internet
* Service registry and discovery support direct, Consul and Zookeeper
* Client side discovery and load balance to eliminate proxies
* A client module that is tightly integrated with Light-OAuth2 and supports traceability

### Design and Test driven development to increase productivity
Design OpenAPI specification and generate the service from it. The specification is also 
part of the framework to drive security verification and request validation at runtime.

Unit/End-to-End test stubs are generated to enable test driven approach for quality product.

Debugging within IDE just like standalone application for better developer productivity.

### Built-in DevOps flow to support continuous integration to production

Dockerfile and DevOps supporting files are generated to support dockerization and continuous
integration to production.

### Multiple frameworks for different type of microservices

light-rest-4j is a RESTful microservice framework with OpenAPI specification for code generation and runtime security and validation
light-graphql-4j is a GraphQL microservice framework that supports schema generation from IDL and plugin.
light-hybrid-4j is a hybrid microservice framework that takes advantages of both monolithic and microservice architectures.
light-eventuate is a messaging based microservice framework based on Kafka, event sourcing and CQRS

### Multiple languages support

All the open sourced frameworks are built in Java and we are working on Nodejs framework internally.
In the future, we might provide Golang framework as well and all them are sharing the same eco-system
and market place. 


### OAuth2 server, portal and services to form ecosystem

[OAuth2 Server](https://github.com/networknt/light-oauth2) for security and [Portal](https://github.com/networknt/light-portal)
for production monitor and management. The portal is also a marketplace to link clients and services 
together. 
