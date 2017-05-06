---
date: 2016-10-18T07:00:45-04:00
title: Getting Started
---

## Code Generation

The easiest way to start your RESTful API project is from OpenAPI specification and 
here is a video to show you how to generate a project from swagger spec.

[light-4j-getting-started](https://youtu.be/xSJhF1LcE0Q)

And here is the [step to step tutorial](https://networknt.github.io/light-4j/example/petstore/) 
for [example-petstore](https://github.com/networknt/light-example-4j/tree/master/petstore)


## Start from one of the example project

This is not recommended but if you don't have OpenAPI specification or
your service has very special requirement that cannot be generated, you 
can find many example projects at a separate repo [light-example-4j](https://github.com/networknt/light-example-4j)
and start by copying one of them.

There are folders in light-example-4j that contains examples:

* rest folder container examples built on top of light-rest-4j framework
* graphql folder contains examples built on top of light-graphql-4j framework
* hybrid folder contains examples built on top of light-hybrid-4j framework
* eventuate folder contains examples built on top of light-eventuate framework


## Building microservices? 

Follow the [microservices tutorial](https://networknt.github.io/light-4j/tutorials/microservices/) 
to build multiple services that communicate each other. The source code for the tutorial
can be found at [https://github.com/networknt/light-example-4j](https://github.com/networknt/light-example-4j)

* api_a is calling api_b and api_c
* api_b is calling api_d
* api_c is an independent service
* api_d is an independent service



