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

The easy way to start your API project is to copy from [undertow-server-demo](https://github.com/networknt/undertow-server-demo)

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




