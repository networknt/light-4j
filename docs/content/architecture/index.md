---
date: 2017-01-25T20:18:44-05:00
title: Architecture
---

light-4j is aiming microservices and it has to be high throughput, low latency, light-weight and
address a lot of cross-cutting concerns at the same time. It is based on Undertow Core Http server
and depending on minimum third party libraries.

The topics here are architectural considerations. In a nutshell, architecture is a type of design
where the focus is quality attributes and wide(er) scope whereas design focuses on functional
requirements and more localized concerns. There is another [section](https://networknt.github.io/light-4j/design/)
for design decisions.

Here is a list of architecture decisions for the framework:

* Designed for [microservices](https://networknt.github.io/light-4j/architecture/microservices/) that can be dockerized and deployed within containers.

* Base on pure HTTP without JavaEE as it has too many problems and is [declining](https://networknt.github.io/light-4j/architecture/jee-is-dead/)
 
* [Security](https://networknt.github.io/light-4j/architecture/security/) first design with OAuth2 integration and distributed verification with embedded distributed gateway.

* How to handler distributed [transaction](https://networknt.github.io/light-4j/architecture/transaction/) in microserivces.

* All components are designed as [plugins](https://networknt.github.io/light-4j/architecture/plugin/) and the framework is easy to be extended and customized.

* Can be [integrated](https://networknt.github.io/light-4j/architecture/integration/) with existing application to protect investment over the year for your organization.

* Support direct calls from microservice to microservice without any [gateway](https://networknt.github.io/light-4j/architecture/gateway/), proxy as they add too much overhead. 

* Service logs will be aggregated with ElasticSearch, LogStash and Kibana with [monitoring and alerting](https://networknt.github.io/light-4j/architecture/monitoring/).

* Built-in [CorrelationId and TraceabilityId](https://networknt.github.io/light-4j/architecture/traceability/) to trace service to service calls in aggregated logs.

* Designed for [scalability](https://networknt.github.io/light-4j/architecture/scalability/) so that you can have thousands instances running at the same time. 
 
* Has its own dependency injection framework so that developers can avoid heavy Spring or Guice as they are [bloated](https://networknt.github.io/light-4j/architecture/spring-is-bloated/) 

* [Elements](https://networknt.github.io/light-4j/architecture/platform/) of API platform. 

* API [category](https://networknt.github.io/light-4j/architecture/category/) and how to choose a framework to build your APIs.