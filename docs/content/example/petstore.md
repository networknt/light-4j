---
date: 2016-10-22T20:40:35-04:00
title: petstore
---

# Introduction

[Petstore](https://github.com/networknt/light-java-example/tree/master/petstore) 
is a generated API project based on OpenAPI specification found [here](http://petstore.swagger.io/v2/swagger.json).

This project will be updated constantly when a new version of Light-Java framework 
is released or any updates in [swagger-codegen](https://github.com/networknt/swagger-codegen).

Here is the command line to generate this project from swagger-codegen directory. It
assumes that you have light-java-example cloned in the same working directory and 
petstore directory is removed. If you keep the existing petstore, it will generate
other files but not handlers and test cases for handlers by default. When you have
new endpoints defined in the specification, then new handlers and handler test cases
will be generated. 

```
java -jar modules/swagger-codegen-cli/target/swagger-codegen-cli.jar generate -i http://petstore.swagger.io/v2/swagger.json -l light-java -o ~/networknt/light-java-example/petstore

```

# Build and Start

To build the generated server.

```
cd ..
cd light-java-example/petstore
mvn install exec:exec
```

Now the server will be started and listens to port 8080. 

# Test

The best tool to test REST API is [Postman](https://chrome.google.com/webstore/detail/postman/fhbjgbiflinjbdggehcddcbncdddomop?hl=en)
It is very easy to set headers and other parameters and save the configuration for future
recall.

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
verification by updating src/main/resources/config/security.json

Here is the default security.json

```
{
  "description": "security configuration",
  "enableVerifyJwt": false,
  "enableVerifyScope": true,
  "enableMockJwt": false,
  "jwt": {
    "certificate": {
      "100": "oauth/primary.crt",
      "101": "oauth/secondary.crt"
    },
    "clockSkewInSeconds": 60
  },
  "logJwtToken": true,
  "logClientUserScope": false
}

```

And here is the updated security.json

```
{
  "description": "security configuration",
  "enableVerifyJwt": true,
  "enableVerifyScope": true,
  "enableMockJwt": false,
  "jwt": {
    "certificate": {
      "100": "oauth/primary.crt",
      "101": "oauth/secondary.crt"
    },
    "clockSkewInSeconds": 60
  },
  "logJwtToken": true,
  "logClientUserScope": false
}

```

The updated security.json enabled JWT signature verification as well as scope
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
curl -H "Authorization: Bearer eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTc5NDg3MzA1MiwianRpIjoiSjFKdmR1bFFRMUF6cjhTNlJueHEwQSIsImlhdCI6MTQ3OTUxMzA1MiwibmJmIjoxNDc5NTEyOTMyLCJ2ZXJzaW9uIjoiMS4wIiwidXNlcl9pZCI6InN0ZXZlIiwidXNlcl90eXBlIjoiRU1QTE9ZRUUiLCJjbGllbnRfaWQiOiJmN2Q0MjM0OC1jNjQ3LTRlZmItYTUyZC00YzU3ODc0MjFlNzIiLCJzY29wZSI6WyJ3cml0ZTpwZXRzIiwicmVhZDpwZXRzIl19.gUcM-JxNBH7rtoRUlxmaK6P4xZdEOueEqIBNddAAx4SyWSy2sV7d7MjAog6k7bInXzV0PWOZZ-JdgTTSn6jTb4K3Je49BcGz1BRwzTslJIOwmvqyziF3lcg6aF5iWOTjmVEF0zXwMJtMc_IcF9FAA8iQi2s5l0DYgkMrjkQ3fBhWnopgfkzjbCuZU2mHDSQ6DJmomWpnE9hDxBp_lGjsQ73HWNNKN-XmBEzH-nz-K5-2wm_hiCq3d0BXm57VxuL7dxpnIwhOIMAYR04PvYHyz2S-Nu9dw6apenfyKe8-ydVt7KHnnWWmk1ErlFzCHmsDigJz0ku0QX59lM7xY5i4qA" localhost:8080/v2/pet/111`
```

And we have the result:

```
{  "photoUrls" : [ "aeiou" ],  "name" : "doggie",  "id" : 123456789,  "category" : {    "name" : "aeiou",    "id" : 123456789  },  "tags" : [ {    "name" : "aeiou",    "id" : 123456789  } ],  "status" : "aeiou"}
```

# Docker

When petstore is generated, a default Dockerfile is there ready for any customization. Let's
just use it to create a docker image and start a docker container.

```
docker build -t networknt/petstore .
```

Let's start the docker container.

```
docker run -d -p 8080:8080 networknt/example_petstore
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
an account on docker hub. For me, I am going to push it to networknt/petstore.

```
docker images
docker tag 9f0b9fe29c44 networknt/example_petstore:latest
docker push networknt/example_petstore

```

The example_petstore can be found at https://hub.docker.com/u/networknt/dashboard/

And the following command can pull and run the docker image on your local if you have
docker installed.

```
docker run -d -p 8080:8080 networknt/example_petstore
```


# Metric



# Logging

