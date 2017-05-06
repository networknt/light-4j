---
date: 2016-10-20T14:33:53-04:00
title: API Gateway
---

When your organization is thinking about breaking up the big monolithic
application to adopt microservices architecture, chances are there are
some vendors coming to sell their gateway solutions. Why they want to
sell you gateways and do you really need a gateway?

The reason they are eagerly selling you a gateway is because they have 
to monetize their product as soon as possible before it is obsolete. 
The solutions they provided are not truely microservices as there is 
no standalone gateway in the picture of the real microservices 
architecture. Their solution is coming from web services(SOA) design 
and all services behind the gateway are flattened.

The purpose of the gateway is to address cross-cutting concerns in Web
services as these are built on top of JavaEE platform with no capability
to address these concerns inside the application server. The foundation
of JavaEE is Servlet API which treats request and response immutable and
it doesn't provide a way to intercept request and response easyly in 
servlet filter. The servlet API was designed around 2000 and it cannot be
changed due to backward compatibility. This leaves only one option to
address cross-cutting concerns on the network level. Normally JavaEE app
are designed as multiple layers and putting an extra layer as gateway is
very natural. 

Here is a picture of their typical solution in the beginning.

![ms_oauth2_gateway](/images/ms_oauth2_gateway.png)

After awhile, they realized that for every request from client, there 
are two calls from client and api to OAuth2 server and remote calls 
are too heavy.

Then the solution for gateway vendors is to move OAuth2 server inside 
the gateway so that there is no remote calls between gateway and OAuth2 
server for security verification. Here is an updated gateway.

![ms_oauth2_in_gateway](/images/ms_oauth2_in_gateway.png)

With increasing volume, the monolithic gateway becomes bottleneck and 
the only solutions is horizontal scaling. That means you have a cluster 
of gateway instances and gateway becomes a single point of failure. If 
any component fails in gateway, all your APIs are not accessible. It is 
also very hard to scale as it is a big monolithic application with a lot 
of components built in and uses a lot of CPU and memory. 

When you look inside the APIs protected by the gateway, you can see 
these APIs are implemented in JavaEE containers like 
WebLogic/WebSphere/JBoss/SpringBoot etc. and they don't call each 
other. They are simply monolithic JavaEE application packaged in ear 
or war and exposed RESTful APIs. These APIs are normally deployed in Data
Centers and lately moved to cloud. They are not real microservices at 
all. 

Some smart developers attempted to break these big application into 
smaller pieces and move into the direction of microservices but gateway 
became a problem. Let's take a look at how API to API call looks like 
with gateway in the following diagram.

![ms_gateway_api_to_api](/images/ms_gateway_api_to_api.png)

As you can see, when API A calls API B, although both of them are behind 
the gateway, the request has to go in front of gateway to properly 
authenticate/authorize the request. Clearly, the centralized gateway 
design is against the decentralized principle of microservices 
architecture.

In our framework, the solution is to move all the cross cutting concerns 
to the API framework and APIs are built on top of the framework. In other 
words, a distributed gateway. Here is a diagram to show you client calls 
API A and API A calls API B/API C and API B calls API D. 

![ms_distributed_gateway](/images/ms_distributed_gateway.png)


In this architecture, every API instance contains functions from the 
framework and act like a mini gateway embedded. Along with container 
orchestration tools like Kubernetes or Docker Swarm, the traditional 
gateway is replaced. As there is no remote calls between API to gateway, 
all the cross cutting concerns are addressed in the same request/response 
chain. This gives you the best performance for your APIs. Here
is an [tutorial](https://networknt.github.io/light-4j/tutorials/microservices/) 
which implements the above diagram and source code for the four APIs can
be found [here](https://github.com/networknt/light-example-4j)

Our framework is built on top of Undertow http core server which is very 
light and serves 1.4 million "Hello World!" requests on my desktop with 
average response time 2ms. Is it 44 times faster then the most popular 
RESTful container Sprint Boot with embedded Tomcat.

The performance test code can be found in 
[here](https://github.com/networknt/microservices-framework-benchmark)


In the above diagram, OAuth2 server is an independent entity and you 
might ask if it is a bottleneck. I have written another [document](/architecture/security) 
to address it with distributed JWT token verification and client credentials 
token caching and renewal. Basically, the Client module in the framework
caches the client credentials token until it is about to expire then renew
in the background. 

Also, our own OAuth2 server built on top of light-rest-4j and light-hybrid-4j 
frameworks is very fast that it can handle 60K user login to get authorization 
code per second. For access token, it can serve about 700 access tokens in a 
second. It is also open sourced and can be found at [https://github.com/networknt/light-oauth2](https://github.com/networknt/light-oauth2)

