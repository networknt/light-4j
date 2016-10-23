---
date: 2016-03-08T21:07:13+01:00
title: Introduction
type: index
weight: 0
---

[Undertow](http://undertow.io/) is  one of the fastest Java HTTP servers available and
JBoss WildFly is based on it.

Performance comparison with others can be found
at [www.techempower.com](https://www.techempower.com/benchmarks/#section=data-r12&hw=peak&test=plaintext) and
a simple Hello World server got 1.2 million requests per second on my I5 4 CPU desktop.
[Here](https://www.networknt.com/blog/All/CeHJjNRjRiS1dH1qqme2LQ) is a blog that compares
it with Go 1.6

In addition, I have performance comparison with the most popular REST API framework Spring
Boot in [light-java-example](https://github.com/networknt/light-java-example/tree/master/performance)


Although it is fast, reliable and widely used but the programming style is a little
strange for traditional JEE developers as it uses handler chain for request processing.
Of course, you can use servlet contain on top of it but you are losing the performance
edge by a big margin.

In order to make use this server more efficiently, I have built a framework called
[Light](https://github.com/networknt/light) that is based on it with event sourcing
and graph database. The framework is used to host both my sites
[www.networknt.com](www.networknt.com)
and [www.edibleforestgarden.ca](www.edibleforestgarden.ca) on a single ip address.

The light framework is complex and has both backend and frontend with blog, news,
forum and e-commerce built-in. Some developers/users asked me if I could provide a
simple framework that just supports REST API build for backend only.

And this is how undertow framework was born. The framework contains three part that work
together to have a total solution for containerized microservices.

[light-java](https://github.com/networknt/light-java) to build API

[light-oauth2](https://github.com/networknt/light-portal) to control API access

[light-portal](https://github.com/networknt/light-oauth2) to manage clients and APIs

