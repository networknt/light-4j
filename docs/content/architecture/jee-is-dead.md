---
date: 2016-10-09T08:14:57-04:00
title: Microservices - The final nail in the coffin for Java EE
---

When Java was out, big players like IBM, BEA, Oracle etc. saw a great opportunity
to make money as it is a great language for web programming. But how can you make
big money around a programming language? The answer is to build servers on top 
of it and make it complicated so big corporations will pay big bucks for it. That 
is why we have Java EE specs, JSRs, Weblogic, Websphere and other servers.

Large packages are deployed on these servers that are run so slow and used too 
much memory. Development and Debugging within a container was a nightmare for 
developers and they usually paid well to compensating the pain.

Because of resource usage is too high, you could not find public hosting company 
to support Java with a reasonable price tag for a long time. You want to build a 
website in Java, you have to pay big bucks for hosting even you might just use a 
Servlet container.

For a long time, Java was used within enterprises and big corporations as only 
they can afford million-dollar application servers and well paid enterprise level 
developers. I myself have been riding the train since beginning as a Java EE 
consultant☺

In 2003, Rod Johnson released Spring Framework and it allows IoC and POJO for 
development without EJBs. The productivity increment is huge and a lot of 
developers jumped onto it and thrown J2EE EJBs out of the window. The application
server vendors saw this and in Java EE 5, they provided some features to make 
developer's life easier and less painful, today’s [Spring Framework is so 
bloated](https://networknt.github.io/light-4j/architecture/spring-is-bloated/) 
like Java EE containers and it still based on Java EE servlet container which was
designed in early 2000 without considering multiple cores and NIO.

During this period of time, PHP was flying. It used less memory and resource and 
was well supported by hosting companies. Some CMS platform built on PHP like 
WordPress, Drupal etc. drove a lot of open source developers into PHP. Although 
PHP is the most popular language these days, it has its shortcomings. It is slow 
and hard to make it scalable.

In 2009, Ryan Dahl introduced Node.js that supports asynchronous, non-blocking 
and event-driven I/O. This increase the response rate dramatically as the server 
threads are well utilized and the throughput of a single server can be comparable 
to a cluster of Java EE servers. Node.js is a very good design but it has its 
[limitations](https://networknt.github.io/light-4j/benchmarks/nodejs/). 
It is hard to scale and hard to integrate with existing legacy systems.


In 2014, a new player Undertow came in town and it is Java based non-blocking web 
server. From techempower.com [test](https://www.techempower.com/benchmarks/#section=data-r12&hw=peak&test=plaintext), 
it serves millions requests per second on a single $8000 dell server using the same 
test case Google claimed to serve 1 million requests with a cluster. It is lightweight 
with the core coming under 1Mb and a simple embedded server uses less than 4Mb of 
heap space. 

With the new Undertow Core, we've built [Light 4J Framework](https://github.com/networknt/light-4j) 
which is aiming containerized microserivces. It supports design driven approach 
from OpenAPI specification for RESTful API, GraphQL IDL for GraphQL service and our
home grown schema for Hybrid service with code generation and runtime model based
validation and security.


## Java EE vendors

Years ago, Java EE vendors like Oracle and IBM spent billions dollars to develop their 
application servers and these servers (WebLogic and WebSphere) will be sold for millions
dollars to big organizations. Now it is hard to sell these servers as JBoss is grabbing
market share quickly and Oracle is [dropping Java EE support](https://developers.slashdot.org/story/16/07/02/1639241/oracle-may-have-stopped-funding-and-developing-java-ee)

With microservices gainning traction, the application servers are hard to sell as these
servers are used to host monolithic applications. It doesn't make sense to host a service
with only 200 lines of code on WebSphere which has several millions lines of code. 
99 percent of CPU and memory will be wasted on the Java EE server and your service will be
slow as a snail. This forces them to rebrand and make changes on their platform to allow 
user to build microservices but the result is not promising. I have tested JBoss WildFly
Swarm in my [benchmarks](https://github.com/networknt/microservices-framework-benchmark)
and it is at the bottom. WebLogic Multitenant and WebSphere Liberty will be much worse as
they are significant bigger than WildFly Swarm.
  
  
## Java EE customers

From customer's perspective, it is not worth buying these applications as all the promises
of Java EE are not true. You build an application for WebSphere cannot be deployed on WebLogic
and you have to spend money to upgrade your application to newer version of the application
server as the old version is not supported anymore. And these upgrade cost millions of
dollars plus the cost of the new application servers.
 
Some smart people start to ask questions. Why we need to deploy our application to these
monster servers? Why we need to package our application as ear or war instead of just a 
jar? Why cannot we break the big application to smaller pieces and deploy and scale them 
independently.

## Microservices

The answer for these questions are microservices. Wikipedia defines microservices as 
"...a software architecture style in which complex applications are composed of small, 
independent processes communicating with each other using language-agnostic APIs. 
These services are small, highly decoupled and focus on doing a small task, 
facilitating a modular approach to system-building."

Microservice Architecture make applications easier to build by breaking down application
into services. The services are composable. Each service can be deployed and developed 
separately. The services can be composed into an application. The services have the 
possibility of being used in other applications more readily. This can speed up 
development as services can define an interface and then the services can be developed 
concurrently.

Another reason services make sense is resilience and scalability. Instead of depending 
on a single server and a single deployment, services can be spread around multiple 
machines, data centers or availability zones. If a service fails, you can start up 
another. Since the application is decomposed into microservices (small services), 
you can more efficiently scale it by spinning up more instances of the heaviest used 
services.

If you have lived through COM, DCOM, CORBA, EJBs, OSGi, SOAP, SOA etc. then you 
know the idea of services and components is not a new thing. The issue with enterprise 
components is they assume the use of hardware servers which are large monoliths and 
you want to run a lot of things on the same server. We have EJBs, WAR files and EAR 
files, and all sorts of nifty components and archives because server acquisition was 
a lot more difficult. Well turns out in recent years, that makes no sense. Operating 
systems and servers are ephemeral, virtualized resources and can be shipped like a 
component. We have EC2, OpenStack, Vagrant and Docker. The world changed. Microservice 
Architecture just recognize this trend so you are not developing like you did when the 
hardware, cloud orchestration, multi-cores, and virtualization did not exist.

Don’t use an EAR file or a WAR file when you start a new project.  Now you can run a 
JVM in a Docker image which is just a process pretending to be an OS running in an OS 
that could be running in the cloud which is running inside of a virtual machine which 
is running in Linux server that you don’t own that you share with people who you don’t 
know. Got a busy season? Well then, spin up 100 more server instances for a few weeks 
or hours. This is why you run Java microservices as standalone processes and not running 
inside of a Java EE container, not even a servlet container. 
 
Microservice generally provide an API endpoint over HTTP/JSON. This allows easy 
integration with not only services you build, but any software (open-source or from 
vendor) that provides an HTTP/JSON interface. This makes the services consumable and 
composable in ways that just make sense. A prime example of this is EC2, S3 and other 
services from Amazon (and others). The very infrastructure you deploy on can become 
part of the application and is programmable. 

When you design your application to use microservices, you are designing it to be 
modular, programmable and composable. This allows you to replace microservices with 
other microservices. You can rewrite or improve parts of your larger application 
without disruption. When everything has a programmable API, communications between 
application microservices becomes easier. 

  
While microservices are getting popular, a lot vendors are trying to re-brand their 
Java EE based web services to microservices in order to sell their obsolete product. 
[API Gateway](https://networknt.github.io/light-4j/architecture/gateway/) is one of 
them. These gateways are designed for Web Services but not Microservices. 


Jason Bloomberg, president of Intellyx, talks about the distinction between a typical 
web service and a microservice, arguing against the tendency to try to simply rebrand 
web services as microservices in this [article](http://techbeacon.com/dangers-microservices-washing-get-value-strip-away-hype)

Microservices are not Web Services on enterprise service buses (ESBs). And it is not
the traditional service-oriented architecture (SOA), while it inherits some of the 
basic principles of SOA, it's fundamentally a different set of practices because the 
entire environment has completely transformed.

The environment for microservices architecture, in contrast, is the borderless 
enterprise: end-to-end, cloud-centric digital applications leveraging fully 
virtualized and containerized infrastructure. Containers take applications and 
services down to a self-contained, component level, and DevOps provides the framework 
for the IT infrastructure and automation to develop, deploy, and manage the 
environment.

Microservices don't require containers (or vice versa), but they're easily 
containerizable by design. Furthermore, if you're implementing containers, 
it's difficult and typically unwise to put any new executable code other than 
microservices in them.

Docker and other container technologies are viewed by some as a integral to microservice 
architecture and some confuse and conflate containers with microservices. Containers are 
minimalist OS pieces to run your microservice on. Docker provides ease of development and 
enables easier integration testing. 

Containers are just an enabler to microservices and you can do microservice development 
without containers. And you can use Docker to deploy monolithic application. Microservices 
and containers like Docker go well together. However, Microservices are a lot more than 
containers! 

## Conclusion

As application development style has been changing over the recent years, microservices
are getting more and more popular. Big corporations are breaking their big applications
up to smaller pieces that can be individually deployed and replaced. These smaller
services are deployed within docker containers on the cloud. I myself have been working
on this area for my clients for the last couple of years and devoted my time to build
an open source microservices framework [Light 4J](https://github.com/networknt/light-4j)
which provides all cross cutting concerns for microservices running in containers. It
supports design driven approach and developers will only focus on the domain business
logic in generated handlers. All the rest will be handled by the Framework and DevOps flow.
So far, it is one of the fastest microservices framework available. 




