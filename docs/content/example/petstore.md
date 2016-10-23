---
date: 2016-10-22T20:40:35-04:00
title: petstore
---

# Introduction

[Petstore](https://github.com/networknt/light-java-example/tree/master/petstore) is a generated API project based on OpenAPI specification found [here](http://petstore.swagger.io/v2/swagger.json).

This project will be updated constantly when a new version of light-java framework released or any updates in swagger-codegen.

Here is the command line to generate this project from swagger-codegen directory.

```
java -jar modules/swagger-codegen-cli/target/swagger-codegen-cli.jar generate -i http://petstore.swagger.io/v2/swagger.json -l light-java -o ~/networknt/light-java-example/petstore

```
