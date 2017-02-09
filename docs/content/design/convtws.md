---
date: 2017-02-09T10:59:44-05:00
title: Convert WebServices to Microservices
---

While you are talking about microservices, chances are you existing application
is built as web services. These days a lot of people and vendors are calling
these web services as microservices and it is not right.

The following diagram shows what is the difference between web service
and microservices.

![web_micro_service](/images/web_micro_service.jpeg)


As you can see the traditional web servers are flattened behind an API gateway 
and they are normally build on top of Java EE platform with JAXRS 1.1 or 2.0

In essence, each web service is still a big monolithic Java EE application and
does everything in a war or ear file. Although they expose RESTful API but they
are not microservices. 

On the right side, the same payment API is broken up to microservices and the
payment API in the picture acts as a facade API which has static ip address and
can be accessed from F5 (or other reverse proxy and load balancer) directly. 
