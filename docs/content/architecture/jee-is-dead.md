---
date: 2016-10-09T08:14:57-04:00
title: jee is dead
---

## History

When Java was out, big players like IBM, BEA, Oracle etc. saw a great opportunity
to make money as it is a great language for web programming. But how can you make
big money around a programming language? The answer is to build servers on top 
of it and make it complicated so big corporations will pay big bucks for it. That 
is why we have JEE specs, JSRs, Weblogic, Websphere and other servers.

Large packages are deployed on these servers that are run so slow and used too 
much memory. Development and Debugging within a container was a nightmare for 
developers and they usually paid well to compensating the pain.

Because of resource usage is too high, you could not find public hosting company 
to support Java with a reasonable price tag for a long time. You want to build a 
website in Java, you have to pay big bucks for hosting even you might just use a 
Servlet container.

For a long time, Java was used within enterprises and big corporations as only 
they can afford million-dollar application servers and well paid enterprise level 
developers. I myself have been riding the train since beginning as a JEE 
consultant☺

In 2003, Rod Johnson released Spring Framework and it allows IoC and POJO for 
development without EJBs. The productivity increment is huge and a lot of 
developers jumped onto it and thrown J2EE EJBs out of the window. The application
server vendors saw this and in JEE5, they provided some features to make developer 
more productive and less painful. Unfortunately, today’s [Spring Framework is so 
bloated](https://networknt.github.io/light-java/architecture/spring-is-bloated/) 
like JEE containers and it still based on JEE servlet container which was
designed over ten years ago without considering multiple cores and NIO.

During this period of time, PHP was flying. It used less memory and resource and 
was well supported by hosting companies. Some CMS platform built on PHP like 
WordPress, Drupal etc. drove a lot of open source developers into PHP. Although 
PHP is the most popular language these days, it has its shortcomings. It is slow 
and hard to make it scalable.

In 2009, Ryan Dahl introduced Node.js that supports asynchronous, non-blocking 
and event-driven I/O. This increase the response rate dramatically as the server 
threads are well utilized and the throughput of a single server can be comparable 
to a cluster of JEE servers. Node.js is a very good design but it has its 
limitations. It is hard to scale and hard to integrate with existing legacy systems.


In 2014, a new player Undertow came in town and it is Java based non-blocking web 
server. From techempower.com [test](https://www.techempower.com/benchmarks/#section=data-r12&hw=peak&test=plaintext), 
it serves millions requests per second 
on a single $8000 dell server using the same test case Google claimed to serve 
1 million requests with a cluster. It is lightweight with the core coming under 
1Mb and a simple embedded server uses less than 4Mb of heap space. 

With the new Undertow Core, we've built Light Java Framework which is aiming containerized
microserivces. It supports design driven approach from OpenAPI specification to 
generate code and drives security and validation during runtime. 

## JEE vendors

Years ago, JEE vendors like Oracle and IBM spent billions dollars to develop their 
application servers and these servers (WebLogic and WebSphere) will be sold for millions
dollars to big organizations. Now it is hard to sell these servers as JBoss is grabbing
market share quickly and Oracle is [dropping JEE support](https://developers.slashdot.org/story/16/07/02/1639241/oracle-may-have-stopped-funding-and-developing-java-ee)

With microservices gainning traction, the application servers are hard to sell as these
servers are used to host monolithic applications which is hard to manage. I used to
work on an application that have hundreds of EJBs and it took 45 minutes to build and deploy
on WebLogic to test a single line of change. 
 
  
## JEE customers

From customer's perspective, it is not worth buying these applications as all the promises
of JEE are not true. You build an application for WebSphere cannot be deployed on WebLogic
and you have to spend money to upgrade your application to newer version of the application
server as the old version is not supported anymore. And these upgrade cost millions of
dollars plus the cost of the new application servers.
 
Some smart people start to ask questions. Why we need to deploy our application to these
monster servers? Why we need to package our application as ear or war instead of just a 
jar? Why cannot we break the big application to smaller pieces and deploy and scale them 
independently.

## Microservices

The answer for these questions is microservices. Wikipedia defines microservices as 
"...a software architecture style in which complex applications are composed of small, 
independent processes communicating with each other using language-agnostic APIs. 
These services are small, highly decoupled and focus on doing a small task, 
facilitating a modular approach to system-building."


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

## Conclusion

As application development style has been changing over the recent years, microservices
are getting more and more popular. Big corporations are breaking their big applications
up to smaller pieces that can be individually deployed and replaced. These smaller
services are deployed within docker containers on the cloud. I myself have been working
on this area for my clients for the last couple of years and devoted my time to build
an open source microservices framework [Light Java](https://github.com/networknt/light-java)
which provides all cross cutting concerns for microservices running in containers. It
supports design driven approach and developers will only focus on the domain business
logic in generated handlers. All the rest will be handled by the Framework and DevOps flow.



