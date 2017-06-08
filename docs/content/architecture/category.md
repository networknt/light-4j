---
date: 2017-06-07T12:58:56-04:00
title: API category and framework
---

When an organization pursue microservices, the services/APIs can be grouped
into the following categories. For each category, certain microservice style
is more suitable than others. 

## Open Web APIs

This is also called public API. Companies want to bring their business online
to attract developers to build applications against their public APIs. In this
way, they can extend their business and reach more customers than before. 

### Rest

When doing so, it seems RESTful API is a natural choose as it is well known by
almost every developer and support almost every language. It is the most mature
technology and has support from a lot of commercial infrastructure and tools.

[light-rest-4j](https://github.com/networknt/light-rest-4j) is designed to build 
microservices in RESTful style on top of OpenAPI specification with most 
cross-cutting concerns addressed and allow developer to focus on the domain business 
logic only. 

### Graphql

Since Facebook open sourced Graphql, it is get popular in the open source community
very fast and some companies start to adopt it to replace RESTful APIs for public
APIs. Github.com is one of the example as they are moving to Graphql from Restful.

[light-graphql-4j](https://github.com/networknt/light-graphql-4j) is designed to
build Graphql API with Graphql IDL(Interface Definition Language). Developers just
need to wire in the domain logic into the generated graphql schema to make the
API works. Sometime, Graphql APIs will be used an aggregate API in front of several
RESTful or Hybrid APIs to serve different consumers like Web Server, Native Mobile
and Single Page Application on Browser. 

## B2B APIs

This type of APIs is designed to exchange information and collaborate with partners.
Due to the close relationship with partners, there are more flexibility in term of
choosing more efficient approach to build APIs. It is easier to train developers due
to the close relationship. 

### Rest

Rest is very popular with this type of APIs.

### Graphql

Graphql is gaining traction in certain business scenarios.

### Hybrid

Another option for partner API is RPC style of API which is much more efficient than
RESTful. 

[light-hybrid-4j](https://github.com/networknt/light-hybrid-4j) is designed to build
RPC based microservices. Also, it allows you to build modularized monolithic services
as well. You can build a monolithic services in the beginning and then split one or
more hot services to another instance easily. 


## Internal APIs

Internal APIs will only be used within the same organization and service might be shared
between LOBs. 

### Rest

Rest is very popular with this type of APIs.

### Graphql

Graphql is gaining traction in certain business scenarios.


### Hybrid

Hybrid is one of the frameworks that is very suitable in product APIs. It is highly
recommended.

### Eventuate

Above three microservices frameworks are all designed for request/response type of
interaction between consumers and services. In a data rich environment it has its
drawbacks in dealing with transactions. Distributed transaction is not the answer
in microservices world. Ultimately, each service should have its own database. So
no shared database is allowed. In this case, the eventual consistency microservices
framework is needed to glue all services together to allow data synch between them.

[light-eventuate-4j](https://github.com/networknt/light-eventuate-4j) is designed to 
build microservices based on messaging, event sourcing and CQRS on top of Kafka. 


## Product APIs

Product APIs are used only around one specific product and they are not supposed to be
shared. 

### Rest

Rest is very popular with this type of APIs but it is not very efficient.

### Graphql

Graphql is gaining traction in certain business scenarios.

### Hybrid

Hybrid is one of the frameworks that is very suitable in product APIs. It is highly
recommended.

### Eventuate

Like Internal APIs, Eventual consistency with messaging, event sourcing and CQRS can
be used to glue all services/APIs together.
