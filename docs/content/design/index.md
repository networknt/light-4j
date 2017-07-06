---
date: 2017-02-09T10:57:47-05:00
title: Design
---

light-4j is aiming microservices and design of microservices is more art other
than technique. When you break a business application to smaller pieces, there
is a threshold. If it too fine-grained, then the performance will suffer, but
if it is too coarse-grained, then the benefit of microservices will be diminishing.

The big portion of design work for microservices is to functionally divide an
application to multiple services and define the communication contract between
these services with OpenAPI specifications.


Here is a list of design decisions for microservices:

* [Microservice Design Patterns](http://blog.arungupta.me/microservice-design-patterns/)

* [How to convert existing web services to microservices](https://networknt.github.io/light-4j/design/convtws/)

* [Service Evolution](https://networknt.github.io/light-4j/design/evolution/)

* [How to handle Partial Faiure](https://networknt.github.io/light-4j/design/partial-failure/)

* [Idempotency](https://networknt.github.io/light-4j/design/idempotency/)

* [Consumer Contract and Consumer Driven Contract](https://networknt.github.io/light-4j/design/consumer-contract/)

Work-in-progress:
* [How to design microservices for brand new project/product](https://networknt.github.io/light-4j/design/newprod/)

* [How to replace monolithic Java EE application to microservices](https://networknt.github.io/light-4j/design/monojee/)
