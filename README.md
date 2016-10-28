A fast and configurable microservices framework for docker containers on the cloud.

[Developer Chat](https://gitter.im/networknt/light-java) |
[Documentation](https://networknt.github.io/light-java) |
[Contribution Guide](CONTRIBUTING.md) |

[![Build Status](https://travis-ci.org/networknt/light-java.svg?branch=master)](https://travis-ci.org/networknt/light-java)


[Undertow](http://undertow.io/) is  one of the fastest Java HTTP servers available 
and JBoss WildFly is based on it. 

Performance comparison with others can be found
at [www.techempower.com](https://www.techempower.com/benchmarks/#section=data-r12&hw=peak&test=plaintext) and
a simple Hello World server got 1.45 million requests per second on my I5 4 CPU desktop.
[Here](https://www.networknt.com/blog/All/CeHJjNRjRiS1dH1qqme2LQ) is a blog that compares it with Go 1.6.

Although it is fast, reliable and widely used but the programming style is a little strange for traditional
JEE developers as it uses handler chain for request processing. Of course, you can use servlet contain on top of it
but you are losing the performance edge. Here is the
[performance test](https://github.com/networknt/light-java-example/tree/master/performance) between Sprint Boot
embedded Undertow and Undertow Server with all middleware turned off.

In order to make use this server more efficiently, I have built a framework
called [Light](https://github.com/networknt/light) that is based on it with event sourcing
and graph database. The framework is used to host both my sites [www.networknt.com](www.networknt.com)
and [www.edibleforestgarden.ca](www.edibleforestgarden.ca) on a single ip address.

The light framework is complex and has both backend and frontend(ReactJS) with blog, news, forum and e-commerce builtin.
Some developers/users asked me if I could provide a simple framework that just supports API build for backend only. And
here is the simplified Undertow Server. With Undertow-OAuth2 and Undertow-Portal in the picture, three of them together
are call Undertow Framework.


## Getting Started

There are two ways to start your project:

### Swagger code generator
If you have OpenAPI/Swagger specification, then you can use
[swagger-codegen](https://networknt.github.io/light-java/tools/swagger-codegen/) to generate a working project.
This is the recommended way to start your REST API project. Here are the steps:

```
clone https://github.com/networknt/swagger-codegen
cd swagger-codegen
mvn clean install -DskipTests
java -jar modules/swagger-codegen-cli/target/swagger-codegen-cli.jar generate -i http://petstore.swagger.io/v2/swagger.json -l light-java -o samples/server/petstore/mypetstore
cd samples/server/petstore/mypetstore
mvn install exec:exec

```

Please replace the input spec and output folder accordingly.

The server is up and running on port 8080 by default and OAuth JWT verification is off by default.

User your browser/postman to access your endpoints and you will have a message returned.

Here is a video to show you the steps to use generator to start your project.

https://www.youtube.com/watch?v=xSJhF1LcE0Q


### Starting from example project

The other way to start your project is to copy from
[petstore](https://github.com/networknt/light-java-example/tree/master/petstore) project in 
light-java-example.


```
mvn clean install exec:exec
```

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

### Copmmand Line

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

