---
date: 2016-10-22T20:48:04-04:00
title: microservices
---

# Introduction

This example contains four projects:

[API A](https://github.com/networknt/light-java-example/tree/master/api_a) is calling [API B](https://github.com/networknt/light-java-example/tree/master/api_b) and [API C](https://github.com/networknt/light-java-example/tree/master/api_c)

API B is calling [API D](https://github.com/networknt/light-java-example/tree/master/api_d)


This example shows:

* How to build microservices
* How to do API to API call with light-java client component
* How to protect API with JWT token with scopes
* How to performance test APIs with wrk

There is a [tutorial](https://networknt.github.io/light-java/tutorials/microservices/) for these projects.

Note: these projects are not 100% completed yet. I am going to dockerize them and then compose them with OAuth2 server.

