---
date: 2016-10-22T20:40:35-04:00
title: petstore
---

# Introduction

[Petstore](https://github.com/networknt/light-example-4j/tree/master/petstore) 
is a generated RESTful API project based on OpenAPI specification found [here](https://github.com/networknt/model-config/tree/master/rest/petstore).


# Prepare Environment

You need to have Java JDK 8 (I prefer OpenJDK but Oracle JDK will do), Maven, Git and 
Docker installed before starting this tutorial.
 
Assuming above software packages are installed, let's create a workspace and clone the 
projects we need for the tutorial.

```
cd ~
mkdir workspace
cd workspace
git clone git@github.com:networknt/light-codegen.git
git clone git@github.com:networknt/light-example-4j.git
git clone git@github.com:networknt/light-oauth2.git
git clone git@github.com:networknt/light-docker.git
git clone git@github.com:networknt/model-config.git
```

We are going to re-generate petstore project in light-example-4j. So let's rename
the existing directory in light-example-4j to petstore.bak

```
cd light-example-4j
mv petstore petstore.bak
cd ..
```

Now let's build the light-codegen to make it ready to generate petstore project. There are
ways to use light-codegen but here we just build and use the command line codgen-cli.

```
cd light-codegen
mvn install -DskipTests
```


# Generate project

This project will be updated constantly when a new version of Light-4J framework 
is released or any updates in [light-codegen](https://github.com/networknt/light-codegen).

Here is the command line to generate this project from model-config directory. It
assumes that you have light-example-4j cloned in the same working directory and 
petstore directory is removed or renamed. If you keep the existing petstore, it will 
generate other files but not handlers and test cases for handlers by default. When 
you have new endpoints defined in the specification, you'd better to generate to
another directory and then merge two project together. 

```
java -jar target/codegen-cli.jar -f light-rest-4j -o ~/workspace/light-example-4j/petstore -m ~/workspace/model-config/rest/petstore/swagger.json -c ~/workspace/model-config/rest/petstore/config.json
```

# Build and Start

To build the generated server.

```
cd ..
cd light-example-4j/petstore
mvn install exec:exec
```

Now the server will be started and listens to port 8080. 

# Test

The best tool to test RESTful API is [Postman](https://chrome.google.com/webstore/detail/postman/fhbjgbiflinjbdggehcddcbncdddomop?hl=en)
It is very easy to set headers and other parameters and save the configuration for 
future usage.

Some people like curl command line and it works as well. Here is one example to access
one of the endpoint petstore serves. 

```
curl localhost:8080/v2/pet/111
```

And the result looks like this. This is the generated example response based on swagger
specification.

```
{  "photoUrls" : [ "aeiou" ],  "name" : "doggie",  "id" : 123456789,  "category" : {    "name" : "aeiou",    "id" : 123456789  },  "tags" : [ {    "name" : "aeiou",    "id" : 123456789  } ],  "status" : "aeiou"}
```

Now, let's test the server with an url that is not defined in the specification.

```
curl localhost:8080/abc
```
And the result is:

```
{"statusCode":404,"code":"ERR10007","message":"INVALID_REQUEST_PATH","description":"Request path cannot be found in the spec."}
```

In fact, the specification is loaded into the framework at runtime and there is a
module called Validator that does the validation against specification for headers,
query parameters, uri parameters and body. It also supports validation using JSON
schema with a independent library [json-schema-validator](https://github.com/networknt/json-schema-validator)

# Enable secrity

By default, the generated API has security turned off. You an turn on the JWT 
verification by updating src/main/resources/config/security.yml in petstore project.

Here is the default security.yml

```
# Security configuration in light framework.
---
# Enable JWT verification flag.
enableVerifyJwt: false

# Enable JWT scope verification. Only valid when enableVerifyJwt is true.
enableVerifyScope: true

# User for test only. should be always be false on official environment.
enableMockJwt: false

# JWT signature public certificates. kid and certificate path mappings.
jwt:
  certificate:
    '100': oauth/primary.crt
    '101': oauth/secondary.crt
  clockSkewInSeconds: 60

# Enable or disable JWT token logging
logJwtToken: true

# Enable or disable client_id, user_id and scope logging.
logClientUserScope: false
```

And here is the updated security.json

```
# Security configuration in light framework.
---
# Enable JWT verification flag.
enableVerifyJwt: true

# Enable JWT scope verification. Only valid when enableVerifyJwt is true.
enableVerifyScope: true

# User for test only. should be always be false on official environment.
enableMockJwt: false

# JWT signature public certificates. kid and certificate path mappings.
jwt:
  certificate:
    '100': oauth/primary.crt
    '101': oauth/secondary.crt
  clockSkewInSeconds: 60

# Enable or disable JWT token logging
logJwtToken: true

# Enable or disable client_id, user_id and scope logging.
logClientUserScope: false
```

The updated security.yml enabled JWT signature verification as well as scope
verification. 

Let's shutdown the server by issuing a CTRL+C on the terminal. And restart the server
after repackage it.

```
mvn package exec:exec
```

With the security enabled, the curl command won't work anymore. 

```
curl localhost:8080/v2/pet/111
```

Result:

```
{"statusCode":401,"code":"ERR10002","message":"MISSING_AUTH_TOKEN","description":"No Authorization header or the token is not bearer type"}
```

In order to access it, you have to provide the right JWT token. There is a long lived
token that can be found at [https://github.com/networknt/light-oauth2](https://github.com/networknt/light-oauth2)

Let's use that token in curl.

```
curl -H "Authorization: Bearer eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTc5NDg3MzA1MiwianRpIjoiSjFKdmR1bFFRMUF6cjhTNlJueHEwQSIsImlhdCI6MTQ3OTUxMzA1MiwibmJmIjoxNDc5NTEyOTMyLCJ2ZXJzaW9uIjoiMS4wIiwidXNlcl9pZCI6InN0ZXZlIiwidXNlcl90eXBlIjoiRU1QTE9ZRUUiLCJjbGllbnRfaWQiOiJmN2Q0MjM0OC1jNjQ3LTRlZmItYTUyZC00YzU3ODc0MjFlNzIiLCJzY29wZSI6WyJ3cml0ZTpwZXRzIiwicmVhZDpwZXRzIl19.gUcM-JxNBH7rtoRUlxmaK6P4xZdEOueEqIBNddAAx4SyWSy2sV7d7MjAog6k7bInXzV0PWOZZ-JdgTTSn6jTb4K3Je49BcGz1BRwzTslJIOwmvqyziF3lcg6aF5iWOTjmVEF0zXwMJtMc_IcF9FAA8iQi2s5l0DYgkMrjkQ3fBhWnopgfkzjbCuZU2mHDSQ6DJmomWpnE9hDxBp_lGjsQ73HWNNKN-XmBEzH-nz-K5-2wm_hiCq3d0BXm57VxuL7dxpnIwhOIMAYR04PvYHyz2S-Nu9dw6apenfyKe8-ydVt7KHnnWWmk1ErlFzCHmsDigJz0ku0QX59lM7xY5i4qA" localhost:8080/v2/pet/111
```

And we have the result:

```
{  "photoUrls" : [ "aeiou" ],  "name" : "doggie",  "id" : 123456789,  "category" : {    "name" : "aeiou",    "id" : 123456789  },  "tags" : [ {    "name" : "aeiou",    "id" : 123456789  } ],  "status" : "aeiou"}
```

# Docker

When petstore is generated, a default Dockerfile is there ready for any customization. 
Let's just use it to create a docker image and start a docker container. Make sure you
are in light-example-4j/petstore folder.

```
docker build -t networknt/example-petstore .
```

Let's start the docker container.

```
docker run -d -p 8080:8080 networknt/example-petstore
```

In another terminal, run the curl to access the server.

```
curl -H "Authorization: Bearer eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTc5NDg3MzA1MiwianRpIjoiSjFKdmR1bFFRMUF6cjhTNlJueHEwQSIsImlhdCI6MTQ3OTUxMzA1MiwibmJmIjoxNDc5NTEyOTMyLCJ2ZXJzaW9uIjoiMS4wIiwidXNlcl9pZCI6InN0ZXZlIiwidXNlcl90eXBlIjoiRU1QTE9ZRUUiLCJjbGllbnRfaWQiOiJmN2Q0MjM0OC1jNjQ3LTRlZmItYTUyZC00YzU3ODc0MjFlNzIiLCJzY29wZSI6WyJ3cml0ZTpwZXRzIiwicmVhZDpwZXRzIl19.gUcM-JxNBH7rtoRUlxmaK6P4xZdEOueEqIBNddAAx4SyWSy2sV7d7MjAog6k7bInXzV0PWOZZ-JdgTTSn6jTb4K3Je49BcGz1BRwzTslJIOwmvqyziF3lcg6aF5iWOTjmVEF0zXwMJtMc_IcF9FAA8iQi2s5l0DYgkMrjkQ3fBhWnopgfkzjbCuZU2mHDSQ6DJmomWpnE9hDxBp_lGjsQ73HWNNKN-XmBEzH-nz-K5-2wm_hiCq3d0BXm57VxuL7dxpnIwhOIMAYR04PvYHyz2S-Nu9dw6apenfyKe8-ydVt7KHnnWWmk1ErlFzCHmsDigJz0ku0QX59lM7xY5i4qA" localhost:8080/v2/pet/111
```

And the result should be:

```
{  "photoUrls" : [ "aeiou" ],  "name" : "doggie",  "id" : 123456789,  "category" : {    "name" : "aeiou",    "id" : 123456789  },  "tags" : [ {    "name" : "aeiou",    "id" : 123456789  } ],  "status" : "aeiou"}
```

Now let's shutdown the docker container. First use "docker ps" to find the container_id
and then issue docker stop with that container_id.

```
docker ps
docker stop ad86cc533270
```

The next step, let's push the docker image to docker hub. This assumes that you have
an account on docker hub. For me, I am going to push it to networknt/example-petstore.

Please skip this step if you don't have a docker hub account yet.

```
docker images
docker tag 9f0b9fe29c44 networknt/example-petstore:latest
docker push networknt/example-petstore

```

The example-petstore can be found at https://hub.docker.com/u/networknt/dashboard/

And the following command can pull and run the docker image on your local if you have
docker installed.

```
docker run -d -p 8080:8080 networknt/example-petstore
```


# Metric

In order to use oauth2(light-oauth2), metrics(Influxdb and Grafana) and 
logging(Elasticsearch, Logstash and Kibana), we've cloned the light-docker repo.


Now let's start all services defined in docker-compose.yml

```
cd ~/workspace/light-docker
docker-compose up --build
```

First let's make sure that Influxdb and Grafana can be accessed.

```
http://localhost:8083/
```
Make sure Influxdb admin page is shown up and metrics database is created.

```
http://localhost:3000/
```

Make sure Grafana dashboard is up and you can login with admin/admin.

Once both Influxdb and Grafana are up and running, let's stop the example-petstore
container by issuing "docker ps" on another terminal to find out the container_id of
example-petstore. Note: there are several docker containers running so double check
you have picked the right container_id.

Now run the following command to stop the example-petstore

```
docker stop [container_id of example-petstore]
```

In the next step, we are going to start the same container with externalized metrics
config so that the server can connect to the Influxdb container to report the metrics.

Let's create a folder under /tmp and name it petstore. Within /tmp/petstore, create
another folder called config. Now create metrics.yml in /tmp/petstore/config folder.

```
description: Metrics handler configuration
enabled: true
influxdbProtocol: http
influxdbHost: influxdb
influxdbPort: 8086
influxdbName: metrics
influxdbUser: root
influxdbPass: root
reportInMinutes: 1
```

Please note that the above configuration is only for testing. 

Now start the container and linked to Influxdb. 

```
docker run -d -p 8080:8080 -v /tmp/petstore/config:/config --network=lightdocker_light networknt/example-petstore
```
Access the one endpoint several times with curl command and wait for one minute.

```
curl -H "Authorization: Bearer eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTc5NDg3MzA1MiwianRpIjoiSjFKdmR1bFFRMUF6cjhTNlJueHEwQSIsImlhdCI6MTQ3OTUxMzA1MiwibmJmIjoxNDc5NTEyOTMyLCJ2ZXJzaW9uIjoiMS4wIiwidXNlcl9pZCI6InN0ZXZlIiwidXNlcl90eXBlIjoiRU1QTE9ZRUUiLCJjbGllbnRfaWQiOiJmN2Q0MjM0OC1jNjQ3LTRlZmItYTUyZC00YzU3ODc0MjFlNzIiLCJzY29wZSI6WyJ3cml0ZTpwZXRzIiwicmVhZDpwZXRzIl19.gUcM-JxNBH7rtoRUlxmaK6P4xZdEOueEqIBNddAAx4SyWSy2sV7d7MjAog6k7bInXzV0PWOZZ-JdgTTSn6jTb4K3Je49BcGz1BRwzTslJIOwmvqyziF3lcg6aF5iWOTjmVEF0zXwMJtMc_IcF9FAA8iQi2s5l0DYgkMrjkQ3fBhWnopgfkzjbCuZU2mHDSQ6DJmomWpnE9hDxBp_lGjsQ73HWNNKN-XmBEzH-nz-K5-2wm_hiCq3d0BXm57VxuL7dxpnIwhOIMAYR04PvYHyz2S-Nu9dw6apenfyKe8-ydVt7KHnnWWmk1ErlFzCHmsDigJz0ku0QX59lM7xY5i4qA" localhost:8080/v2/pet/111
```

Go to http://localhost:8083/ and select metrics database and select "SHOW MEASUREMENTS". 
You will find several measurements created. Some of them are for api view and some of them
are for client view. 

You can connect Grafana to Influxdb and create dashboards on Grafana to visualize
the metrics. 

# Logging

Logging is very important in microservices architecture as logs must be aggregated in
order to trace all the activities of a particular request from a consumer. We are using
ELK stack for logging. In the above step, Elasticsearch, Logstash and Kibana are all
started in the same docker-compose.yml.

In order for the example-petstore container to forward log files to ELK, we need to
set up log driver on the application container to forward logs to Logstash.
 

Here is the command line to start the example-petstore
 
```
export LOGSTASH_ADDRESS=$(docker inspect --format '{{ .NetworkSettings.Networks.lightdocker_light.IPAddress }}' lightdocker_logstash_1)
docker run -d -p 8080:8080 -v /tmp/petstore/config:/config --network=lightdocker_light --log-driver=gelf --log-opt gelf-address=udp://$LOGSTASH_ADDRESS:12201 --log-opt tag="petstore" --log-opt env=dev networknt/example-petstore

```

Now you example-petstore is up and running and all logs have sent to the Logstash and 
then ElasticSearch. Let's go to the Kibana to see if our logs can be viewed there.

```
http://localhost:5601/
```

Just select the default template and you can see the logs there.

# Summary

This conclude the petstore example on the dev environment; however, the steps are not for
production as there are a lot of security issues need to be addressed and containers
won't be managed by docker-compose on production. Kubernetes or Docker Swarm will be utilized
likely. This is out of the scope for this tutorial for now. 


