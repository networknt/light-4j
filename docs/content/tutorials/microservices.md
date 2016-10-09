---
date: 2016-10-09T08:01:56-04:00
title: microservices
---

## Introduction

These days light weight container like Docker is getting traction, more and more API services are developed for
docker container and deployed to the cloud. In this environment, traditional heavy weight containers like
JEE and Spring are losing ground as it doesn't make sense to have a heavy weight container wrapped with a light
weight docker container. Docker and container orchestration tools like Kubenetes or Docker Swarm are replacing
all the functionalities JEE provides without hogging resources.


Another clear trend is standalone Gateway is phasing out in the cloud
environment with docker containers as most of the traditional gateway
features are replaced by container orchestration tool and docker container
management tool. In addition, some of the cross cutting concerns gateway
provided are addressed in API framework.

## Specifications

Undertow Server Framework encourage Design Driven API building and [OpenAPI
Specification](https://github.com/OAI/OpenAPI-Specification) is the central
piece to drive the runtime for security and validation. Also, the
specification can be used to scaffold a running server project the first time
so that developers can focus their efforts on the domain business logic
implementation without worrying about how each components wired together.

To create swagger specification, the best tool is
[swagger-editor](http://swagger.io/swagger-editor/) and I have an
[article]() in
tools section to describe how to use it.

By following the [instructions]()
on how to use the editor, let's create four APIs in swagger repo.

API A will call API B and API C to fulfill its request and API B will call API D
to fulfill its request.

```
API A -> API B -> API D
         -> API C
```

Here is the API A swagger.yaml and others can be found [here](https://github.com/networknt/swagger)

```
swagger: '2.0'

info:
  version: "1.0.0"
  title: API A for microservices demo
  description: API A is called by consumer directly and it will call API B and API C to get data
  contact:
    email: stevehu@gmail.com
  license:
    name: "Apache 2.0"
    url: "http://www.apache.org/licenses/LICENSE-2.0.html"
host: a.networknt.com
schemes:
  - http
basePath: /v1

consumes:
  - application/json
produces:
  - application/json

paths:
  /data:
    get:
      description: Return an array of strings collected from down stream APIs
      operationId: listData
      responses:
        200:
          description: Successful response
          schema:
            title: ArrayOfStrings
            type: array
            items:
              type: string
      security:
        - a_auth:
          - api_a.w
          - api_a.r

securityDefinitions:
  a_auth:
    type: oauth2
    authorizationUrl: http://localhost:8080/oauth2/code
    flow: implicit
    scopes:
      api_a.w: write access
      api_a.r: read access
```

## Swagger-Codegen



## Handlers

## Configuration

## Dockerization

## OAuth2 Security

## Integration

## Performance

## Production

## Conclusion

