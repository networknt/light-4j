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
curl -H "Authorization: Bearer eyJraWQiOiIxMDAiLCJhbGciOiJSUJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTc5NDgwMDYzOSwianRpIjoiWFhlQmpJYXUwUk5ZSTl3dVF0MWxtUSIsImlhdCI6MTQ3OTQ0MDYzOSwibmJmIjoxNDc5NDQwNTE5LCJ2ZXJzaW9uIjoiMS4wIiwidXNlcl9pZCI6InN0ZXZlIiwidXNlcl90eXBlIjoiRU1QTE9ZRUUiLCJjbGllbnRfaWQiOiJmN2Q0MjM0OC1jNjQ3LTRlZmItYTUyZC00YzU3ODc0MjFlNzIiLCJzY29wZSI6WyJ3cml0ZTpwZXRzIiwicmVhZDpwZXRzIl19.f5XdkmhOoHT2lgTobqVGPp2aWUv_ItA0tqyLHC_CeMbmwzPvREqb5-oJ9T_m3VwRcJlPTh8xTdSjrLITXClaQFE4Y0bT8C-u6bb38uT-NQ5mjUjLrFQYHCF6GqwL7YkwQt_rshEqtrDFe1T4HoEL_9FHbOxf3MSJ39UKq0Ef_9mHXkn4Y-SHfdapeuUWc_4dDPdxzEdzbqmf1WSOOgTuM5O5F2fK4p_ix8LQl0H3AnMZIhIDyygQEnYPxEG-u35gwh503wfxio6buIf0b2Kku2PXPE36lethZwIVaPTncEcY5OPxfBxXuy-Wq-YQizd7NnpJTteHYbdQXupjK7NDvQ" localhost:8080/v2/pet/111
```

And we have the result:

```
{  "photoUrls" : [ "aeiou" ],  "name" : "doggie",  "id" : 123456789,  "category" : {    "name" : "aeiou",    "id" : 123456789  },  "tags" : [ {    "name" : "aeiou",    "id" : 123456789  } ],  "status" : "aeiou"}
```

# Docker


# Metric


# Logging

