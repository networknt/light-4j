A fast and configurable REST microservices framework for docker containers on the cloud.

[Developer Chat](https://gitter.im/networknt/light-java) |
[Documentation](https://networknt.github.io/light-java) |
[Contribution Guide](CONTRIBUTING.md) |

[![Build Status](https://travis-ci.org/networknt/light-java.svg?branch=master)](https://travis-ci.org/networknt/light-java)


## Why this framework

### Fast and small memory footprint to lower production cost.

It is 44 times faster than the most popular microservices platform Spring Boot embedded 
Tomcat and use only 1/5 of memory. Here is the [benchmark](https://github.com/networknt/light-java-example/tree/master/performance) 
results compare with Spring Boot and other microservices frameworks.

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

### Design and Test driven development to increase productivity
Design OpenAPI specification and generate the service from it. The specification is also part
of the framework to drive security verification and request validation.

Unit/End-to-End test stubs are generated to enable test driven approach for quality product.

Debugging within IDE just like standalone application for better developer productivity

### Built-in DevOps flow to support continuous integration to production

Dockerfile and DevOps supporting files are generated to support dockerization

### OAuth2 server, portal and services to form ecosystem

[OAuth2 Server](https://github.com/networknt/light-oauth2) and [Portal](https://github.com/networknt/light-portal)
are working in progress for future production monitoring and management.


## Getting Started

There are two ways to start your project:

### Swagger code generator
If you have OpenAPI/Swagger specification, then you can use
[swagger-codegen](https://networknt.github.io/light-java/tools/swagger-codegen/) to generate a working project.
This is the recommended way to start your REST API project. Here are the steps:

```
git clone https://github.com/networknt/swagger-codegen
cd swagger-codegen
mvn clean install -DskipTests
java -jar modules/swagger-codegen-cli/target/swagger-codegen-cli.jar generate -i http://petstore.swagger.io/v2/swagger.json -l light-java -o samples/server/petstore/mypetstore
cd samples/server/petstore/mypetstore
mvn install exec:exec

```

Please replace the input spec and output folder accordingly.

The server is up and running on port 8080 by default and OAuth JWT verification is off by default.

Use your browser/postman to access server endpoints and you will have a generated 
example message returned if the endpoint has a response body.

Here is a [step by step tutorial](https://networknt.github.io/light-java/example/petstore/) for [example-petstore](https://github.com/networknt/light-java-example/tree/master/petstore) 

And here is a video to show you the steps for above tutorial.

https://youtu.be/hYXz1lR6NKU

Petstore is a single API, for microservices that involves multiple APIs or services,
please follow [microservices tutorial](https://networknt.github.io/light-java/tutorials/microservices/)


### Starting from example project

The other way to start your project is to copy from
[petstore](https://github.com/networknt/light-java-example/tree/master/petstore) project in 
[light-java-example](https://github.com/networknt/light-java-example).


```
git clone git@github.com:networknt/light-java-example.git
cd light-java-example/petstore
mvn clean install exec:exec
```
In another terminal, issue the curl command like this to verify the server is
working. 

```
curl localhost:8080/v2/pet/111
```

## Debugging

[To run/debug from IDE](https://networknt.github.io/light-java/tutorials/debug/), you need to 
configure a Java application with main class "com.networknt.server.Server" and working 
directory is your project folder. There is no container and you are working on just a standalone
Java application.


## Start Server

### In IDE
create a Java application that main class is com.networknt.server.Server and working
directory is your project root folder. You can debug your server just like a single 
POJO application.

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
complete in-flight requests and close the database connections etc.

