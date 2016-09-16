# Java Undertow Server

## Introduction

[Undertow](http://undertow.io/) is  one of the fastest Java HTTP servers available and JBoss WildFly is based on it.

Performance comparison with others can be found
at [www.techempower.com](https://www.techempower.com/benchmarks/#section=data-r12&hw=peak&test=plaintext) and
a simple Hello World server got 1.2 million requests per second on my I5 4 CPU desktop.
[Here](https://www.networknt.com/blog/All/CeHJjNRjRiS1dH1qqme2LQ) is a blog that compares it with Go 1.6.

Although it is fast, reliable and widely used but the programming style is a little strange for traditional
JEE developers as it uses handler chain for request processing. Of course, you can use servlet contain on top of it
but you are losing the performance edge.

In order to make use this server more efficiently, I have built a framework
called [Light](https://github.com/networknt/light) that is based on it with event sourcing
and graph database. The framework is used to host both my sites [www.networknt.com](www.networknt.com)
and [www.edibleforestgarden.ca](www.edibleforestgarden.ca) on a sigle ip address.

The light framework is complex and has both backend and frontend(ReactJS) with blog, news, forum and e-commerce builtin.
Some developers/users asked me if I could provide a simple framework that just supports API build for backend only.

And here you go.

## Getting Started

There are two ways to start your project:

### Swagger code generator
If you have swagger yaml spec, then you can use swagger-codege to generate a working project. This is the recommended
way to start your REST API project. Here are the steps:

```
clone https://github.com/networknt/swagger-codegen
cd swagger-codegen
mvn clean install -DskipTests
java -jar modules/swagger-codegen-cli/target/swagger-codegen-cli.jar generate -i http://petstore.swagger.io/v2/swagger.json -l undertow -o samples/server/petstore/mypetstore
cd samples/server/petstore/mypetstore
mvn install exec:exec

```
Please replace the input spec and output folder accordingly.

The server is up and running on port 8080 by default and OAuth JWT verification is off by default.

User your browser/postman to access your endpoints and you will have a message returned.


### Starting from demo or example project

The other way to start your project is to copy from [undertow-server-demo](https://github.com/networknt/undertow-server-demo)
or project from [undertow-server-example](https://github.com/networknt/undertow-server-example)

Example repository contains all sort of examples for different type of servers that are not REST APIs with
swagger spec.

The server is using SPI to find root handler in your project so you have to create one class that implements com.networknt.server.HandlerProvider
DemoHandlerProvider is an example. It basically create a HttpHandler and add all your routes to it. For every endpoints, you can
have a separate handler to handle the request.

In order for undertow-server to find your HandlerProvider implementation, you have to create a file under
resources/META-INF/services named com.networknt.server.HandlerProvider

In the file, put the full name of your implementation class. For example.

com.networknt.demo.handler.DemoHandlerProvider

To start the server from command line,

```
mvn clean install exec:exec
```

For production, there is another way to start the server and will be documented later.

To run/debug from IDE, you need to configure a Java application with main class "com.networknt.server.Server" and
working directory is your project folder. There is no contain and you are working on just a standalone Java application.

Regarding to hwo to access the server you just started, please refer to the README from
[undertow-server-demo](https://github.com/networknt/undertow-server-demo)


## Configuration

Almost every server component has a configuration file in JSON format and a default config file is located in resources/config.

This allows all components can be used out of the box but that might not be ideal for you. If you want to change the config, you
can create a folder resources/config in your project and overwrite the default config of each components. One example is the
security.json in undertow-server-demo project to turn off the JWT token verification.

For offcial deployment, you should copy your config files to a directory in file system and use a system properties to point
to that folder for your config.

For example, create a folder in /home/steve/config and put all updated config files there. In order to let your server to lookup
that folder for config, you need to pass in -Dundertow-server-config-dir=/home/steve/config when you start the server. Another way
to do that is to put JAVA_TOOL_OPTIONS in .bashrc file

export JAVA_TOOL_OPTIONS='-Dundertow-server-config-dir=/home/steve/config'

One example you want to update config is to turn off OAuth2 JWT token verification on the server so that you can test your
endpoints without putting token in the request header. In this case, you need to copy security.json from security module to
/home/steve/config folder and update enableVerifyJwt to false.

Note that you have to restart your terminal if you add JAVA_TOOL_OPTIONS to .bashrc and restart your IDE from the new terminal
window.

## Start Server

### In IDE
create a Java application that main class is com.networknt.server.Server and working
directory is your project root folder. You can debug your server just like a single POJO application.

### From Maven

mvn exec:exec

### Copmmand Line

For testing
```
java -jar target/demo-0.1.0.jar
```

For production

```
setsid java -jar demo-0.1.0.jar < /dev/zero & > /dev/null &
```

## Stop Server

you can use Ctrl+C to kill the server but for production use the following command

```
kill -s TERM <pid>
```

The server has a shutdown hook and the above command allow it to clean up. For example,
complete in-flight requests and close the database connections etc.

