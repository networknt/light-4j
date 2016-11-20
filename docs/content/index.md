---
date: 2016-03-08T21:07:13+01:00
title: Introduction
type: index
weight: 0
---

## Why this framework

### Fast and small memory footprint to lower production cost.

It is 44 times faster than the most popular microservices platform Spring Boot embedded 
Tomcat and use only 1/5 of memory. Here is the [benchmark](https://github.com/networknt/light-java-example/tree/master/performance) 
results compare with Spring Boot, Nodejs and Golang.

### Provide middleware components to address cross cutting concerns.
* Plugin architecture for startup/shutdown hooks and middleware components
* Distributed OAuth2 JWT security verification as part of the framework
* Request and response validation against OpenAPI specification at runtime
* Metrics collected in influxdb and viewed from Grafana Dashboard for both services and clients
* Global exception handling for runtime exception, api exception and other checked exception
* Mask sensitive data like credit card, sin number etc. before logging
* Sanitize cross site scripting for request header and body
* Audit to dump important info or entire request and response.
* Body parser to support different content types
* Standardized response code and messages from configuration file
* Externalized configuration for all modules for dockerized environment 

### Design/Test driven development to increase productivity
Design OpenAPI specification and generate the service from it. The specification is also part
of the framework to drive security verification and request validation.

Unit/End-to-End tests to enable test driven approach for quality product.

Debugging within IDE just like standalone application for better developer productivity

### Built-in DevOps flow to support continuous integration to production

Dockerfile and DevOps supporting files are generated to support dockerization

### OAuth2 server, portal and light Java to form ecosystem

[light-java](https://github.com/networknt/light-java) to build API

[light-oauth2](https://github.com/networknt/light-oauth2) to control API access

[light-portal](https://github.com/networknt/light-portal) to manage clients and APIs

