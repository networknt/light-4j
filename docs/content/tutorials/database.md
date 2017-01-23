---
date: 2017-01-23T09:07:32-05:00
title: Database Access Tutorial
---

# Introduction

Most microservices will have to access database in order to fulfill consumer requests. 
In this tutorial, we will walk through the following steps with Oracle/Postgres/Mysql:

* How to setup database connection pool
* How to connect to the database instance
* How to do query database tables
* How to update database tables

# Preparation

In order to follow the steps below, please make sure you have the same working environment.

* A computer with MacOS or Linux (Windows should work but I never tried)
* Install git
* Install Docker
* Install JDK 8 and Maven
* Create a working directory under your user directory called networknt.


# Create Database Demo Specification

First let's build an OpenAPI specification with several endpoints to demo database
access. You will need [swagger editor](tools/swagger-editor/)
to create a specification. 

Here is the OpenAPI specification created and it can be found in 
[swagger repo](https://github.com/networknt/swagger) database sub folder.

```
swagger: '2.0'

info:
  version: "1.0.0"
  title: Light-Java-Rest Database Tutorial
  description: A demo on how to connect, query and update Oracle/Mysql/Postgres. 
  contact:
    email: stevehu@gmail.com
  license:
    name: "Apache 2.0"
    url: "http://www.apache.org/licenses/LICENSE-2.0.html"
host: database.networknt.com
schemes:
  - http
  - https
basePath: /v1

consumes:
  - application/json
produces:
  - application/json

paths:
  /query:
    get:
      description: Single query to database table
      operationId: getQuery
      responses:
        200:
          description: "successful operation"
          schema:
            $ref: "#/definitions/RandomNumber"          
      security:
      - database_auth:
        - "database.r"
  /queries:
    get:
      description: Multiple queries to database table
      operationId: getQueries
      parameters:
      - name: "queries"
        in: "query"
        description: "Number of random numbers"
        required: false
        type: "integer"
        format: "int32"
      responses:
        200:
          description: "successful operation"
          schema:
            type: "array"
            items:
              $ref: "#/definitions/RandomNumber"
      security:
      - database_auth:
        - "database.r"
  /updates:
    get:
      description: Multiple updates to database table
      operationId: getUpdates
      parameters:
      - name: "queries"
        in: "query"
        description: "Number of random numbers"
        required: false
        type: "integer"
        format: "int32"
      responses:
        200:
          description: "successful operation"
          schema:
            type: "array"
            items:
              $ref: "#/definitions/RandomNumber"
      security:
      - database_auth:
        - "database.w"
securityDefinitions:
  database_auth:
    type: "oauth2"
    authorizationUrl: "http://localhost:8888/oauth2/code"
    flow: "implicit"
    scopes:
      database.w: "write database table"
      database.r: "read database table"
definitions:
  RandomNumber:
    type: "object"
    required:
    - "id"
    - "randomNumber"
    properties:
      id:
        type: "integer"
        format: "int32"
        description: "a unique id as primary key"
      randomNumber:
        type: "integer"
        format: "int32"
        description: "a random number"
```

Now let's clone the swagger repo to your working directory.

```
cd ~/networknt
git clone https://github.com/networknt/swagger
```

# Generate Demo Project

With the specification in place, we can generate the code with [swagger-codegen](https://github.com/networknt/swagger-codegen)

There are two different ways to generate the code:

* Local build
* Docker container

To learn how to use the tool, please refer to this [document](tools/swagger-codegen/)

### Generate code with local build

Clone and build swagger-codegen

```
cd ~/networknt
git clone git@github.com:networknt/swagger-codegen.git
cd swagger-codegen
mvn clean install -DskipTests
```

For this demo, I am going to generate the code into light-java-example/database/generated
folder so that users can check the code later on from this repo. 

Let's checkout the light-java-example repo and backup the existing database project.

```
cd ~/networknt
git clone git@github.com:networknt/light-java-example.git
cd light-java-example
mv database database.bak
mkdir database
cd database
mkdir generated
```

Before generating the project, we need to create a config.json to define packages,
artifactId and groupId for the project.

Here is the content of the file and it can be found in ~/networknt/swagger/database

```
{
  "invokerPackage": "com.networknt.database",
  "apiPackage":"com.networknt.database.handler",
  "modelPackage":"com.networknt.database.model",
  "artifactId": "database",
  "groupId": "com.networknt"
}
```


Code generation

```
cd ~/networknt/swagger-codegen
java -jar modules/swagger-codegen-cli/target/swagger-codegen-cli.jar generate -c ~/networknt/swagger/database/config.json -i ~/networknt/swagger/database/swagger.yaml -l light-java -o ~/networknt/light-java-example/database/generated

```

Now you should have a project generated. Let's build it and run it.

```
cd ~/networknt
cd light-java-example/database/generated
mvn clean install exec:exec
```

Now you can access the service with curl following the step below.


### Generate code with docker container

Let's remove the generated folder from light-java-example/database folder and
generate the project again with docker container.

```
cd ~/networknt/light-java-example/database
rm -rf generated
```

Now let's generate the project again with docker.

```
cd ~/networknt
docker run -it -v ~/networknt/swagger/database:/swagger-api/swagger -v ~/networknt/light-java-example/database:/swagger-api/out networknt/swagger-codegen generate -c /swagger-api/swagger/config.json -i /swagger-api/swagger/swagger.yaml -l light-java -o /swagger-api/out/generated

```

Let's build and start the service

```
cd ~/networknt/light-java-example/database/generated
mvn clean install exec:exec
```

Now you can access the service with curl following the next step.


### Test the service

Now the service is up and running. Let's access it from curl

Single query

```
curl http://localhost:8080/v1/query

{  "randomNumber" : 123,  "id" : 123}
```

Multiple queries with default number of object returned

```
curl http://localhost:8080/v1/queries

[ {  "randomNumber" : 123,  "id" : 123} ]
```

Multiple queries with 10 numbers returned

```
curl http://localhost:8080/v1/queries?queries=10

[ {  "randomNumber" : 123,  "id" : 123} ]
```

Multiple updates with default number of object updated

```
curl http://localhost:8080/v1/updates

[ {  "randomNumber" : 123,  "id" : 123} ]
```


Multiple updates with 10 numbers updated

```
curl http://localhost:8080/v1/updates?queries=10

[ {  "randomNumber" : 123,  "id" : 123} ]
```


# Setup Connection Pool

