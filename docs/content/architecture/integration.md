---
date: 2016-11-12T20:55:44-05:00
title: Integration Patterns
---

While working with my clients to transform monolithic Java EE applications to 
microservices architecture, one of the most frequently asked questions from 
my clients is how do you integrate your newly built microservices with existing
Java EE applications. In other words, how to leverage existing application
stacks when exposing REST APIs with microservices?

For most organizations especially financial institutions, they have big Java
EE applications running on Weblogic/Websphere that they've invested efforts
for a decade or longer. You cannot image that they can rewrite everything and
switch to microservices overnight. 

I have been working the following four different approaches over the last 5 
years and I will give my recommendations based on my experience. Please be
aware that this is just a generic recommendation and it cannot be applied
to all use cases. 

# API Gateway


Most commercial API gateways offer the XML to JSON and JSON to XML 
transformation feature and this was good selling point to in early days. They
promised that you buy their product and the gateway will transform your 
XML based web services to JSON based RESTful APIs. The problem with this
approach is performance, as all of them provide a generic transformer working
with external defined mapping logic. The transformation is CPU intensive and
with the overhead of gateway security and other layers, the throughput and
latency are not acceptable. Also, there are other issues with commercial 
gateways as I documented at [https://networknt.github.io/light-4j/architecture/gateway/](https://networknt.github.io/light-4j/architecture/gateway/)

# Customized Transformation in API

Some developers and architects realized that buying a gateway cannot resolve
the problem magically so they tried to build the transform logic into the API
itself. The transformation code is customized per API and it is much more
efficient than generic transformer in gateways. This provide a little bit
more flexibility and a little bit increased performance but it is not easy
to write the transform code as most web services have very complicated request
and response schemas. Performance wise, it is better than commercial gateway
solutions but still very bad.

# Calling service layer behind SOA or EMB

Most web services are built with multiple layers and chances are you have
a service layer behind your web service tier with Java native APIs. In this
case, we can bypass web services and calling the native Java API (most likely
session beans) from your RESTful APIs. This gives us a relative good performance
and leverage the most complicated services in the application tier. It is also
a low cost solution to bring RESTful API on top of your existing applications. 

The only drawback is that these app layers are deployed on Java EE platform
and they have limited throughput and very hard to be scaled. 


# Calling Book of Record directly

In order to fully realize the benefits of microservices architecture, the
existing monolithic application must be rewritten so that microservices can
talk to the book of record directly. And this can be done during a long period
of time to break up existing system to function areas and convert them one by
one. Unlike the way Jave EE works to have different layers in designed, we need
to cut the function vertically for microservices. 

# Summary

As describe above, it is recommended to rewrite the existing monolithic app
but if resources are constrained, then build microservices by calling 
existing services are acceptable. As microserivces can be individually deployed
and replaced, it is easy to convert them all to the final stage one by one.



