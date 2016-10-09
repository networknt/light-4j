---
date: 2016-10-08T22:14:33-04:00
title: Swagger Editor for OpenAPI Specification
---

## Introduction

[Swagger](http://swagger.io/) is a simple yet powerful representation of your RESTful API. With the largest
ecosystem of API tooling on the planet, thousands of developers are supporting Swagger in almost every modern
programming language and deployment environment. With a Swagger-enabled API, you get interactive documentation,
client SDK generation and discoverability.

Swagger was created to help fulfill the promise of APIs and is 100% open source software.

[The Swagger specification](http://swagger.io/specification/) defines a set of files required to describe an API.
These files can then be used by the Swagger-UI project to display the API and Swagger-codegen to generate clients
or servers in various languages. Additional utilities can also take advantage of the resulting files, such as
testing tools.

## Swagger Editor

### Specification Management

If you only have one API to be built, you can save your specification anywhere and you don't need any external
references; however, you might work for an organization which is building hundreds or thousands APIs and there
are so many shared references between APIs like common headers, error status etc. In this case, it is recommended
to create a separate repo for specification only. Normally, it will be called swagger.

Within this folder, each api will have its own sub folder and common shared references will be in the root folder.

Swagger spec. can be edited in yaml or json format but most people will be using yaml format and we are stick to it.


### Using the online editor
To access the Swagger online editor, click the following link.

[Online Swagger Editor](http://editor.swagger.io/#/)

  * To create a new specification - File --> New

  * To import an existing specification from the File --> Import File ...

  * To save/export the specification in YAML format - File --> Download YAML.

Online editor work if you are working on only one API without any externalized references. If you want to work with
multiple APIs, you have to run the editor locally.

### Running editor locally

  ```shell
  git clone https://github.com/swagger-api/swagger-editor.git
  cd swagger-editor
  npm install
  npm start
  ```

Now your default browser will be started and point to 127.0.0.1:8080/# with a sample specification loaded.

### Create a new specification

The swagger-editor serves the static files via an HTTP server. To work on API specifications, the simplest way
is to clone the swagger repository directly in the Swagger editor folder.

```shell
cd swagger-editor
git clone https://github.com/networknt/swagger.git
cd swagger
```
When saving/exporting your specification, please use swagger.yaml filename in your API sub folder under swagger.
Common object specifications are located in the root folder of the /swagger repository. Ex. header.yaml,
error.yaml, etc. To refer these common specification files in API specification

```
$ref: 'swagger/error.yaml#/error'
```

### Useful links

[OpenAPI Specification](http://swagger.io/specification/)
[Official Examples](https://github.com/OAI/OpenAPI-Specification/tree/master/examples/v2.0)
[Tutorial](https://apihandyman.io/writing-openapi-swagger-specification-tutorial-part-1-introduction/)





