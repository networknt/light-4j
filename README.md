A fast, lightweight and cloud-native microservices framework.

[Developer Chat](https://gitter.im/networknt/light-4j) |
[Documentation](https://www.networknt.com) |
[Contribution Guide](CONTRIBUTING.md) |

[![Build Status](https://travis-ci.org/networknt/light-4j.svg?branch=master)](https://travis-ci.org/networknt/light-4j)

## Why called Light 4J

Light means lightweight, lightning fast and shedding light on how to program with modern Java SE.   

## Why this framework

### Fast and small memory footprint to lower production cost.

It is 44 times faster than the most popular microservices platform Spring Boot embedded 
Tomcat and use only 1/5 of memory. Here is the [benchmark](https://github.com/networknt/microservices-framework-benchmark) 
results compare with Spring Boot and other microservices frameworks. Here is the [comparison](https://www.techempower.com/benchmarks/#section=data-r15&hw=ph&test=plaintext)
with other Web frameworks. 

### Provide an embedded gateway to address cross-cutting concerns.
* Plugin architecture for startup/shutdown hooks and middleware components
* Distributed OAuth2 JWT security verification as part of the framework
* Request and response validation against OpenAPI specification at runtime
* Metrics collected in Influxdb/Prometheus and viewed from Grafana Dashboard for both services and clients
* Global exception handling for runtime exception, API exception, and other checked exceptions
* Mask sensitive data like the credit card, sin number, etc. before logging
* Sanitize cross-site scripting for query parameters, request headers and body
* Audit to dump important info or entire request and response.
* Body parser to support different content types
* Standardized response code and messages from the configuration file
* Externalized configuration for all modules for the dockerized environment 
* CORS pre-flight handler for SPA (Angular or React) from another domain
* Rate limiting for services that exposed outside to the Internet
* Service registry and discovery support direct, Consul and Zookeeper
* Client-side discovery and load balance to eliminate proxies
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

- [light-rest-4j](https://github.com/networknt/light-rest-4j) is a RESTful microservice framework with OpenAPI specification for code generation and runtime security and validation
- [light-graphql-4j](https://github.com/networknt/light-graphql-4j) is a GraphQL microservice framework that supports schema generation from IDL and plugin.
- [light-hybrid-4j](https://github.com/networknt/light-hybrid-4j) is a hybrid microservice framework that takes advantages of both monolithic and microservice architectures.
- [light-eventuate](https://github.com/networknt/light-eventuate-4j) is a messaging based microservice framework based on Kafka, event sourcing and CQRS

### Multiple languages support

All the open sourced frameworks are built in Java and we are working on Nodejs framework internally.
In the future, we might provide Golang framework as well and all them are sharing the same eco-system
and market place. 


### OAuth2 server, portal and services to form ecosystem

[OAuth2 Server](https://github.com/networknt/light-oauth2) for security and [Portal](https://github.com/networknt/light-portal)
for production monitor and management. The portal is also a marketplace to link clients and services 
together. 


## Getting Started

There are two ways to start your project:

### Light-codegen generator

You can use [light-codegen](https://github.com/networknt/light-codegen) to generate a working project.
Currently, it supports light-rest-4j, light-graphql-4j, light-hybrid-server-4j and light-hybrid-service-4j. 
light-eventuate code generator is coming. 

The light-codegen project README.md describes four ways to use the generator with examples.

* Clone and build the light-codgen and use the codegen-cli command line utility
* Use docker image networknt/light-codegen to run the codegen-cli command line utility
* Use generate.sh from [model-config](https://github.com/networknt/model-config) repo to generate projects based on conventions. 
* Generate code from web site with codegen-web API. (API is ready but UI needs to be built) 


### Starting from an example project

The other way to start your project is to copy from [light-example-4j](https://github.com/networknt/light-example-4j).

You can find the description of these [examples](https://www.networknt.com/example/)  

Also, there are some [tutorials](https://www.networknt.com/tutorial/) 


## Debugging

[To run/debug from IDE](https://www.networknt.com/tutorial/common/debug/), you need to 
configure a Java application with main class "com.networknt.server.Server" and working 
directory is your project folder. There is no container and you are working on just a standalone
Java application.


## Start Server

### In IDE
create a Java application that main class is com.networknt.server.Server and working
directory is your project root folder. You can debug your server just like a POJO application.

### From Maven

mvn exec:exec

### Command Line

```
java -jar target/demo-0.1.0.jar
```

## Stop Server

you can use Ctrl+C to kill the server but for production use the following command

```
kill -s TERM <pid>
```

The server has a shutdown hook and the above command allow it to clean up. For example,
complete in-flight requests and close the database connections etc. If service registry
and discovery is used, then the server will send shutdown event to service registry and
keep processing requests for 30 seconds until all clients refreshes their local cache 
before shutting down. 

## Appreciation

- Light-4j has been optimized by using open source license of [JProfiler](http://www.ej-technologies.com/products/jprofiler/overview.html) 
from [ej-technologies](http://www.ej-technologies.com/). 

## License

Light-4j and all light-*-4j frameworks are available under the Apache 2.0 license. See the [LICENSE](https://www.apache.org/licenses/LICENSE-2.0) 
file for more info.
