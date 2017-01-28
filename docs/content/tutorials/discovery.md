---
date: 2017-01-27T20:57:14-05:00
title: Registry and Discovery
---

# Introduction

This is a tutorial to show you how to use service registry and discovery
for microservices. We are going to use api_a, api_b, api_c and api_d as
our examples.

The specifications for above APIs can be found at 
https://github.com/networknt/swagger

# Preparation

In order to follow the steps below, please make sure you have the same 
working environment.

* A computer with MacOS or Linux (Windows should work but I never tried)
* Install git
* Install Docker
* Install JDK 8 and Maven
* Install Java IDE (Intellij IDEA Community Edition is recommended)
* Create a working directory under your user directory called networknt.

```
cd ~
mkdir networknt
```

# Clone the specifications

In order to generate the initial projects, we need to call swagger-codegen
and we need the specifications for these services.

```
cd ~/networknt
git clone git@github.com:networknt/swagger.git
```

In this repo, you have a generate.sh in the root folder to use docker
container of swagger-codegen to generate the code and there are api_a,
api_b, api_c and api_d folder for swagger.yaml files and config.json
files for each API.

# Code generation

We are going to generate the code into light-java-example repo, so let's
clone this repo into our working directory.

```
cd ~/networknt
git clone git@github.com:networknt/light-java-example.git
```

In the above repo, there is a folder discovery contains all the projects
for this tutorial. In order to start from scratch, let's change the existing
folder to discovery.bak as a backup so that you can compare if your code is
not working in each step.

```
cd ~/networknt/light-java-example
mv discovery discovery.bak
```

Now let's generate the four APIs.

```
cd ~/networknt/swagger
./generate.sh ~/networknt/swagger/api_a ~/networknt/light-java-example/discovery/api_a
./generate.sh ~/networknt/swagger/api_b ~/networknt/light-java-example/discovery/api_b
./generate.sh ~/networknt/swagger/api_c ~/networknt/light-java-example/discovery/api_c
./generate.sh ~/networknt/swagger/api_d ~/networknt/light-java-example/discovery/api_d

```

We have four projects generated and compiled under generated folder under each 
project folder. 

# Test generated code

Now you can test the generated projects to make sure they are working with mock
data. We will pick up one project to test it but you can test them all.

```
cd ~/networknt/light-java-example/discovery/api_a/generated
mvn exec:exec
```

From another terminal, access the server with curl command and check the result.

```
curl http://localhost:8080/v1/data
[ "aeiou" ]
```

At this time, all projects are listening the same port 8080, so you have to shutdown
one server in order to start another one to test them all. The return result should
be the same as they are generated from the similar specifications.

# Static

Now we have four standalone services and the next step is to connect them together.

Here is the call tree for these services.

API A will call API B and API C to fulfill its request. API B will call API D
to fulfill its request.

```
API A -> API B -> API D
      -> API C
```

Before we change the code, let's copy the generated projects to new folders so
that we can compare the changes later on.

```
cd ~/networknt/light-java-example/discovery/api_a
cp -r generated static
cd ~/networknt/light-java-example/discovery/api_b
cp -r generated static
cd ~/networknt/light-java-example/discovery/api_c
cp -r generated static
cd ~/networknt/light-java-example/discovery/api_d
cp -r generated static
```

Let's start update the code in static folders for each project. If you are
using Intellij IDEA Community Edition, you need to open light-java-example
repo and then import each project by right click pom.xml in each static folder.

As indicated from the title, here we are going to hard code urls in API to API
calls. That means these services will be deployed on the known hosts with known
ports. And we will have a config file for each project to define the calling
service urls.

### API A


### API B

### API C

### API D


# Dynamic

# Docker


#


# API to API calls

As we have four API projects created, we can now update them for API to API calls.

Here is the call tree for these services.

API A will call API B and API C to fulfill its request. API B will call API D
to fulfill its request.

```
API A -> API B -> API D
      -> API C
```

Before we change the code, let's copy the generated projects to new folders so
that we can compare the changes.

```
cd ~/networknt/light-java-example/discovery/api_a
cp -r generated apitoapi
cd ~/networknt/light-java-example/discovery/api_b
cp -r generated apitoapi
cd ~/networknt/light-java-example/discovery/api_c
cp -r generated apitoapi
cd ~/networknt/light-java-example/discovery/api_d
cp -r generated apitoapi
```

Let's start update the code in apitoapi folders for each project. If you are
using Intellij IDEA Community Edition, you need to open light-java-example
repo and then import each project by right click pom.xml in each apitoapi folder.

### API A


### API B

### API C

### API D

