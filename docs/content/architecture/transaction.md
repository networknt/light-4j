---
date: 2017-08-10T15:04:40-04:00
title: Transaction management in microservices architecture
---

In traditional monolithic applications, we normally use distributed transactions([XA](https://en.wikipedia.org/wiki/X/Open_XA))
and two-phase commit([2PC](https://en.wikipedia.org/wiki/Two-phase_commit_protocol)) 
to maintain data consistency and integrity.

After moving to microservices architecture, we don't have the option to use 
XA correctly technically; however, a lot of developer will use it implicitly 
without knowing the risk. In fact, they don't even realize that they are using 
distributed transactions between microservices.

## What is a distributed transaction

When we are talking about distributed transaction in monolithic world, we are 
talking about explicit distributed transactions. In microservices architecture
people will not use explicit distributed transactions but they can use implicit
distributed transactions if they are not very careful.

Graham Lea defines distributed transaction in his article - [Distributed Transactions: The Icebergs of Microservices](http://www.grahamlea.com/2016/08/distributed-transactions-microservices-icebergs/)
as: 

Distributed transaction - any situation where a single event results in the 
mutation of two or more separate sources of data which cannot be committed 
atomically.

## Risk of distributed transactions in microservices

A lot of developers use implicit distributed transactions between multiple
microservices and it works most of time until one day there is a small network
glitch and the data of your application is messed up as the application designer
only thinks about happy path. 

This happens when one command from UI triggers two or more microservices to
update their states. If everything is perfect, all updates will be successful
and everybody is happy. But what about the first service update is succeeded and
the second failed due to network issue? Is this acceptable in your business? The
majority of business people will answer no to this question.

So given we are doing microservices, how do we resolve the data consistency issue
between services. There are solutions based on how these services interact with
each other. 


## Solution - request/response 

When service to service communication is synchronous, the options are very limited.
Graham Lea documented some solution in his article and I have written some related
article before. 

- [Idempotency of Microservices](https://networknt.github.io/light-4j/design/idempotency/)
- [How to handler partial failure](https://networknt.github.io/light-4j/design/partial-failure/) 

## Solution - event based

A microservices architecture promotes availability over consistency, hence leveraging 
a distributed transaction coordinator with a two-phase commit protocol is usually not
an option. It gets even more complicated with the idea that these loosely-coupled data 
repositories might not necessarily all be relational stores and could be a polyglot 
with a mix of relational and non-relational data stores. The idea of having an immediately 
consistent state is difficult. What you should look forward to, however, is an architecture 
that promotes eventual consistency like CQRS and Event Sourcing. An event driven architecture 
can support a BASE (basic availability, soft-state, and eventual consistency) transaction 
model. This is a great technique to solve the problem of handling transactions across 
services and customers can be convinced to negotiate the contract towards a BASE model since 
usually transactional consistency across functional boundaries is mostly about frequent 
change of states, and consistency rules can be relaxed to let the final state be eventually 
visible to the consumers.

To enable developer to use event driven approach for service to service communication, we
have provide [light-eventuate-4j](https://github.com/networknt/light-eventuate-4j) framework 
that support Event Sourcing and CQRS with Kafka as event broker. You can build your service
with light-rest-4j, light-graphql-4j or light-hybrid-4j. They will communicate with each
other asynchronously with events and eventually the states of each services will be consistent.

 