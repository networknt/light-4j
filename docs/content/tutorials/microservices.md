---
date: 2016-10-09T08:01:56-04:00
title: Microservices
---

## Introduction

These days light weight container like Docker is getting traction, more and more 
API services are developed for docker container and deployed to the cloud. In this
environment, traditional heavy weight containers like Java EE and Spring are 
losing ground as it doesn't make sense to have a heavy weight container wrapped 
with a light weight docker container. Docker and container orchestration tools 
like Kubernetes or Docker Swarm are replacing all the functionalities Java EE 
provides without hogging resources.


Another clear trend is standalone Gateway is phasing out in the cloud environment 
with docker containers as most of the traditional gateway features are replaced 
by container orchestration tool and docker container management tools. In addition, 
some of the cross cutting concerns gateway provided are addressed in API framework.

## Prepare workspace

All specifications and code of the services are on github.com but we are going to
redo it again by following the steps in the tutorial. Let's first create a
workspace. I have created a directory named networknt under /home/steve.

Checkout related projects.

```
cd networknt
git clone git@github.com:networknt/swagger-codegen.git
git clone git@github.com:networknt/light-java-example.git
git clone git@github.com:networknt/swagger.git
git clone git@github.com:networknt/light-oauth2.git

```

As we are going to regenerate API A, B, C and D, let's rename these folders from
light-java-example.

```
cd light-java-example
mv api_a api_a.bak
mv api_b api_b.bak
mv api_c api_c.bak
mv api_d api_d.bak
cd ..

```

## Specifications

Light Java Microservices Framework encourages Design Driven API building and 
[OpenAPI Specification](https://github.com/OAI/OpenAPI-Specification) is the central
piece to drive the runtime for security and validation. Also, the specification 
can be used to scaffold a running server project the first time so that developers 
can focus their efforts on the domain business logic implementation without 
worrying about how each components wired together.

During the service implementation phase, specification might be changed and you can
regenerate the service codebase again without overwrite your handlers and test
cases for handlers. 

To create swagger specification, the best tool is
[swagger-editor](http://swagger.io/swagger-editor/) and I have an
[article](https://networknt.github.io/light-java/tools/swagger-editor/)
in tools section to describe how to use it.

By following the [instructions](https://networknt.github.io/light-java/tools/swagger-editor/)
on how to use the editor, let's create four APIs in swagger repo.

API A will call API B and API C to fulfill its request and API B will call API D
to fulfill its request.

```
API A -> API B -> API D
      -> API C
```

Here is the API A swagger.yaml and others can be found at
[https://github.com/networknt/swagger](https://github.com/networknt/swagger) or light-oauth2
folder in your workspace. 

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

As defined in the specification, API A will return a list of stings and it requires
scope api_a.r or scope api_a.w to access the endpoint /data.


## Swagger-Codegen

Now we have four API swagger.yaml files available. Let's use swagger-codegen
to start four projects in light-java-example. In normal API build, you 
should create a repo for each API.

#### Build Light Java Generator

As [swagger-codegen](https://github.com/swagger-api/swagger-codegen) doesn't
support Java 8, I have forked it [here](https://github.com/networknt/swagger-codegen)

```
cd swagger-codegen
mvn clean install -DskipTests
```

#### Generate first project

Now you have your swagger-codegen built, let's generate a project. Assume that
swagger, light-java-example and swagger-codegen are in the same working
directory.

```
java -jar modules/swagger-codegen-cli/target/swagger-codegen-cli.jar generate -i ../swagger/api_a/swagger.yaml -l light-java -o ../light-java-example/api_a

```
Here is the generator output.

```
steve@joy:~/networknt/swagger-codegen$ java -jar modules/swagger-codegen-cli/target/swagger-codegen-cli.jar generate -i ../swagger/api_a/swagger.yaml -l light-java -o ../light-java-example/api_a
Picked up JAVA_TOOL_OPTIONS: -Dconfig.dir=/home/steve/config
[main] INFO io.swagger.parser.Swagger20Parser - reading from ../swagger/api_a/swagger.yaml
swaggerio.swagger.models.Swagger@cb84fa41
[main] INFO io.swagger.codegen.AbstractGenerator - writing file /home/steve/networknt/swagger-codegen/../light-java-example/api_a/pom.xml
[main] INFO io.swagger.codegen.AbstractGenerator - writing file /home/steve/networknt/swagger-codegen/../light-java-example/api_a/README.md
[main] INFO io.swagger.codegen.AbstractGenerator - writing file /home/steve/networknt/swagger-codegen/../light-java-example/api_a/.gitignore
[main] INFO io.swagger.codegen.AbstractGenerator - writing file /home/steve/networknt/swagger-codegen/../light-java-example/api_a/src/main/resources/config/swagger.json
[main] INFO io.swagger.codegen.AbstractGenerator - writing file /home/steve/networknt/swagger-codegen/../light-java-example/api_a/src/main/java/io/swagger/handler/PathHandlerProvider.java
[main] INFO io.swagger.codegen.AbstractGenerator - writing file /home/steve/networknt/swagger-codegen/../light-java-example/api_a/src/test/java/io/swagger/handler/PathHandlerProviderTest.java
[main] INFO io.swagger.codegen.AbstractGenerator - writing file /home/steve/networknt/swagger-codegen/../light-java-example/api_a/src/main/resources/META-INF/services/com.networknt.server.HandlerProvider
[main] INFO io.swagger.codegen.AbstractGenerator - writing file /home/steve/networknt/swagger-codegen/../light-java-example/api_a/src/main/resources/META-INF/services/com.networknt.handler.MiddlewareHandler
[main] INFO io.swagger.codegen.DefaultGenerator - writing file /home/steve/networknt/swagger-codegen/../light-java-example/api_a/src/main/resources/config/server.json
[main] INFO io.swagger.codegen.DefaultGenerator - writing file /home/steve/networknt/swagger-codegen/../light-java-example/api_a/src/main/resources/config/security.json
[main] INFO io.swagger.codegen.DefaultGenerator - writing file /home/steve/networknt/swagger-codegen/../light-java-example/api_a/src/main/resources/config/oauth/primary.crt
[main] INFO io.swagger.codegen.DefaultGenerator - writing file /home/steve/networknt/swagger-codegen/../light-java-example/api_a/src/main/resources/config/oauth/secondary.crt
[main] INFO io.swagger.codegen.AbstractGenerator - writing file /home/steve/networknt/swagger-codegen/../light-java-example/api_a/.swagger-codegen-ignore
[main] INFO io.swagger.codegen.AbstractGenerator - writing file /home/steve/networknt/swagger-codegen/../light-java-example/api_a/LICENSE

```

#### Build and run the mock API

And now you have a new project created in light-java-example. Let's build
it and run the test cases. If everything is OK, start the server.

```
cd ..
cd light-java-example/api_a
mvn clean install
mvn exec:exec
```

Let's test the API A by issuing the following command
```
curl localhost:8080/v1/data
```

By default the operationId 'listData' will be returned.

#### Generate other APIs

Follow the above steps to generate other APIs. Make sure you are in swagger_codegen
directory.

```
java -jar modules/swagger-codegen-cli/target/swagger-codegen-cli.jar generate -i ../swagger/api_b/swagger.yaml -l light-java -o ../light-java-example/api_b
java -jar modules/swagger-codegen-cli/target/swagger-codegen-cli.jar generate -i ../swagger/api_c/swagger.yaml -l light-java -o ../light-java-example/api_c
java -jar modules/swagger-codegen-cli/target/swagger-codegen-cli.jar generate -i ../swagger/api_d/swagger.yaml -l light-java -o ../light-java-example/api_d

```

Now you have four APIs generated from four OpenAPI specifications. Let's check
them in.

```
cd ../light-java-example
git add .
git commit -m "checkin 4 apis"
git push origin master
```

## Handlers

Now these APIs are working if you start them and they will output the operationId
defined in the API specification. Let's take a look at the API handler itself
and update it based on our business logic.

#### API D
Let's take a look at the generated PathHandlerProvider.java in
api_d/src/main/java/io/swagger/handler/

```
package io.swagger.handler;

import com.networknt.config.Config;
import com.networknt.server.HandlerProvider;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Methods;

public class PathHandlerProvider implements HandlerProvider {

    public HttpHandler getHandler() {
        HttpHandler handler = Handlers.routing()


            .add(Methods.GET, "/v1/data", new HttpHandler() {
                        public void handleRequest(HttpServerExchange exchange) throws Exception {
                            exchange.getResponseSender().send("listData");
                        }
                    })

        ;
        return handler;
    }
}

```

This is the only class handling the request based on domain business logic.
Because we only have one endpoint /v1/data@get there is only one route added
to the handler chain. And there is a anonymous class mapped to this endpoint.

The generated handler returns "listData" which is the operationId defined in
the specification. Let's update it to an array of string that indicate
messages from API D.

```
package io.swagger.handler;

import com.networknt.config.Config;
import com.networknt.server.HandlerProvider;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Methods;

import java.util.ArrayList;
import java.util.List;

public class PathHandlerProvider implements HandlerProvider {

    public HttpHandler getHandler() {
        HttpHandler handler = Handlers.routing()


            .add(Methods.GET, "/v1/data", new HttpHandler() {
                        public void handleRequest(HttpServerExchange exchange) throws Exception {
                            List<String> messages = new ArrayList<String>();
                            messages.add("API D: Message 1");
                            messages.add("API D: Message 2");
                            exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(messages));
                        }
                    })

        ;
        return handler;
    }
}

```

Now, let's build it and start the server. Make sure there is only one server
started at any time as all servers are listening to the same port.

```
cd api_d
mvn clean package exec:exec
```
Test it with curl.

```
curl localhost:8080/v1/data
```
And the result is

```
["API D: Message 1","API D: Message 2"]
```

#### API C
Let's shutdown API D and update API C PathHandlerProvider to

```
package io.swagger.handler;

import com.networknt.config.Config;
import com.networknt.server.HandlerProvider;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Methods;

import java.util.ArrayList;
import java.util.List;

public class PathHandlerProvider implements HandlerProvider {

    public HttpHandler getHandler() {
        HttpHandler handler = Handlers.routing()


            .add(Methods.GET, "/v1/data", new HttpHandler() {
                        public void handleRequest(HttpServerExchange exchange) throws Exception {
                            List<String> messages = new ArrayList<String>();
                            messages.add("API C: Message 1");
                            messages.add("API C: Message 2");
                            exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(messages));
                        }
                    })

        ;
        return handler;
    }
}

```

The same endpoint will return

```
["API C: Message 1","API C: Message 2"]
```

#### API B

Let's shutdown API C and complete API B. API B will call API D to fulfill its
request so it has Apache Http Client dependency. The generated pom.xml has
httpclient as test scope. Now you need to move it to runtime dependency and
remove '<scope>test</scope'. After update it should look like this.

```
        <dependency>
            <groupId>io.swagger</groupId>
            <artifactId>swagger-annotations</artifactId>
            <version>${version.swagger}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.httpcomponents</groupId>
            <artifactId>httpclient</artifactId>
            <version>${version.httpclient}</version>
        </dependency>

        <!-- Test Dependencies -->
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>${version.junit}</version>
            <scope>test</scope>
        </dependency>

    </dependencies>

```

Update the generated code to this.

```
package io.swagger.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.client.Client;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import com.networknt.server.HandlerProvider;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Methods;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PathHandlerProvider implements HandlerProvider {
    static final Logger logger = LoggerFactory.getLogger(PathHandlerProvider.class);

    String apidUrl = "http://localhost:8083/v1/data";
    public HttpHandler getHandler() {
        HttpHandler handler = Handlers.routing()


            .add(Methods.GET, "/v1/data", new HttpHandler() {
                        public void handleRequest(HttpServerExchange exchange) throws Exception {
                            List<String> list = new ArrayList<String>();
                            try {
                                CloseableHttpClient client = Client.getInstance().getSyncClient();
                                HttpGet httpGet = new HttpGet(apidUrl);
                                CloseableHttpResponse response = client.execute(httpGet);
                                int responseCode = response.getStatusLine().getStatusCode();
                                if(responseCode != 200){
                                    throw new Exception("Failed to call API D: " + responseCode);
                                }
                                List<String> apidList = (List<String>) Config.getInstance().getMapper().readValue(response.getEntity().getContent(),
                                        new TypeReference<List<String>>(){});
                                list.addAll(apidList);
                            } catch (ClientException e) {
                                    throw new Exception("Client Exception: ", e);
                            } catch (IOException e) {
                                    throw new Exception("IOException:", e);
                            }
                            // now add API B specific messages
                            list.add("API B: Message 1");
                            list.add("API B: Message 2");
                            exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(list));
                        }
                    })

        ;
        return handler;
    }
}

```

As API B only calls one API, here sync client is used. As API B will interact
with API D and it requires configuration changes to make it work. Let's wait
until the next step to test it.

#### API A

API A will call API B and API C to fulfill its request. We need to include
Apache AsyncClient in its dependencies. Update the generated pom.xml to
something like this in the end of the dependencies.

```
        <dependency>
            <groupId>io.swagger</groupId>
            <artifactId>swagger-annotations</artifactId>
            <version>${version.swagger}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.httpcomponents</groupId>
            <artifactId>httpasyncclient</artifactId>
            <version>${version.httpasyncclient}</version>
        </dependency>

        <!-- Test Dependencies -->
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>${version.junit}</version>
            <scope>test</scope>
        </dependency>

    </dependencies>

```

Now let's update the generated code to

```
package io.swagger.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.client.Client;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import com.networknt.server.HandlerProvider;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Methods;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class PathHandlerProvider implements HandlerProvider {

    public HttpHandler getHandler() {
        HttpHandler handler = Handlers.routing()


            .add(Methods.GET, "/v1/data", new HttpHandler() {
                        public void handleRequest(HttpServerExchange exchange) throws Exception {
                            List<String> list = new ArrayList<String>();
                            final HttpGet[] requests = new HttpGet[] {
                                    new HttpGet("http://localhost:8081/v1/data"),
                                    new HttpGet("http://localhost:8082/v1/data"),
                            };
                            try {
                                CloseableHttpAsyncClient client = Client.getInstance().getAsyncClient();
                                final CountDownLatch latch = new CountDownLatch(requests.length);
                                for (final HttpGet request: requests) {
                                    client.execute(request, new FutureCallback<HttpResponse>() {
                                        @Override
                                        public void completed(final HttpResponse response) {
                                            latch.countDown();
                                            try {
                                                List<String> apiList = (List<String>) Config.getInstance().getMapper().readValue(response.getEntity().getContent(),
                                                        new TypeReference<List<String>>(){});
                                                list.addAll(apiList);
                                            } catch (IOException e) {

                                            }
                                        }

                                        @Override
                                        public void failed(final Exception ex) {
                                            latch.countDown();
                                        }

                                        @Override
                                        public void cancelled() {
                                            latch.countDown();
                                        }
                                    });
                                }
                                latch.await();
                            } catch (ClientException e) {
                                throw new Exception("ClientException:", e);
                            }
                            // now add API A specific messages
                            list.add("API A: Message 1");
                            list.add("API A: Message 2");
                            exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(list));
                        }
                    })

        ;
        return handler;
    }
}

```

At this moment, we have all four APIs completed but they are not going to be
started at the same time as generated server.json uses the same port 8080. The
next step is to change the configuration and test it out.

## Configuration

Undertow Server has a module called Config and it is responsible to read config
files from environmental property specified directory, classpath, API
resources/config folder and module resources/config folder in that sequence as
default File System based configuration. It can be extended to other config like
config server, distributed cache and http server etc.

To make things simpler, let's update the server.json in API B, C, D to bind to
different port in order to start them on the same localhost.

API B

Find the server.json at api_b/src/main/resources/config and update the content
to

```
{
  "description": "server config",
  "ip": "0.0.0.0",
  "port": 8081
}

```

API C

```
{
  "description": "server config",
  "ip": "0.0.0.0",
  "port": 8082
}

```
API D

```
{
  "description": "server config",
  "ip": "0.0.0.0",
  "port": 8083
}

```

Now let's start all four servers on four terminals and test them out.

API D
```
cd api_d
mvn clean install
mvn exec:exec
```
API C

```
cd ../api_c
mvn clean install exec:exec
```

API B

```
cd ../api_b
mvn clean install exec:exec
```

API A

```
cd ../api_a
mvn clean install exec:exec
```

Now let's call API A and see what we can get.

Start another terminal and run

```
 curl localhost:8080/v1/data
```

And you should see the result like this.

```
["API C: Message 1","API C: Message 2","API D: Message 1","API D: Message 2","API B: Message 1","API B: Message 2","API A: Message 1","API A: Message 2"]
```

## Performance without security

Now let's see if these servers are performing with
[wrk](https://github.com/wg/wrk). To learn how to use it, please see my
article in tools [here](https://networknt.github.io/light-java/tools/wrk-perf/)

Assume you have wrk installed, run the following command.

```
wrk -t4 -c800 -d30s http://localhost:8080/v1/data

```
And here is what I got on my i5 desktop

```
Running 30s test @ http://localhost:8080/v1/data
  4 threads and 1000 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    94.26ms    5.02ms 278.85ms   82.69%
    Req/Sec     2.65k   235.06     4.02k    82.58%
  316197 requests in 30.10s, 72.48MB read
Requests/sec:  10504.94
Transfer/sec:      2.41MB

```
Now let's push the server to its limit

```
 wrk -t4 -c128 -d30s http://localhost:8080 -s pipeline.lua --latency -- /v1/data 16
```

And here is the result without any tuning.

```
Running 30s test @ http://localhost:8080
  4 threads and 128 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    20.19ms   49.37ms 227.18ms    0.00%
    Req/Sec     3.22k     1.73k    5.17k    50.76%
  Latency Distribution
     50%    0.00us
     75%    0.00us
     90%    0.00us
     99%    0.00us
  378848 requests in 30.03s, 87.32MB read
Requests/sec:  12615.16
Transfer/sec:      2.91MB

```

## Enable OAuth2 Security

So far, we've started four servers and tested them successfully; however,
these servers are not protected by OAuth2 JWT tokens as it is turned off
by default in the generated code.

Before we turn on the security, we need to have [undertow-oauth2](https://github.com/networknt/undertow-oauth2)
server up and running so that these servers can get JWT token in real
time.

The easiest way to run undertow-oauth2 server is through docker but let's
build it locally this time.

Go to your working directory and clone the project and start it.

```
git clone git@github.com:networknt/light-oauth2.git
cd light-oauth2
mvn clean package exec:exec
```

Now let's enable the jwt token verification and scope verification for all
APIs except API A. For API A we want to enableVerifyJwt to true but
enableVerifyScope to false so that we can use a long lived token to test from
curl without getting new tokens each time.

Open security.json and update enableVerifyJwt and enableVerifyScope to true. The
following is what looks like for API B, C and D.

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
This is what looks like for API A

```
{
  "description": "security configuration",
  "enableVerifyJwt": true,
  "enableVerifyScope": false,
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

Now make sure that client.json in both API A and API B are configured correctly
to get the right token from undertow-oauth2 server with the correct scopes.

The client_id and client_secret are set up in clients.json in undertow-oauth2
already [here](https://github.com/networknt/undertow-oauth2/blob/master/src/main/resources/config/clients.json)

API A client
```
  "fe5dadd9-34ad-430f-a8f7-c75e81cc5d7b": {
    "client_secret":"GXkHy-1aSPyo4pst8WBWbg",
    "scope": "api_b.r api_b.w api_c.r api_c.w",
    "redirect_uri": "http://localhost:8080/oauth"
  },
```

API B client

```
  "10a0a743-4674-4a9d-8867-db63ad4c8b4e": {
    "client_secret":"tcahI1dvT1OsxXxg-IB_-w",
    "scope": "api_d.r api_d.w",
    "redirect_uri": "http://localhost:8080/oauth"
  }
```

Now let's update client.json on both API A and API B to reflect the above info.
After updates, this is what looks like in API A client.json

```
{
  "description": "client configuration, all timing is milli-second",
  "sync": {
    "maxConnectionTotal": 100,
    "maxConnectionPerRoute": 10,
    "routes": {
      "api.google.com": 20,
      "api.facebook.com": 10
    },
    "timeout": 10000,
    "keepAlive": 15000
  },
  "async": {
    "maxConnectionTotal": 100,
    "maxConnectionPerRoute": 10,
    "routes": {
      "api.google.com": 20,
      "api.facebook.com": 10
    },
    "reactor": {
      "ioThreadCount": 1,
      "connectTimeout": 10000,
      "soTimeout": 10000
    },
    "timeout": 10000,
    "keepAlive": 15000
  },
  "tls": {
    "verifyHostname": false,
    "loadTrustStore": false,
    "trustStore": "trust.keystore",
    "trustPass": "password",
    "loadKeyStore": false,
    "keyStore": "key.jks",
    "keyPass": "password"
  },
  "oauth": {
    "tokenRenewBeforeExpired": 600000,
    "expiredRefreshRetryDelay": 5000,
    "earlyRefreshRetryDelay": 30000,
    "server_url": "http://localhost:8888",
    "timeout": 5000,
    "ignoreSSLErrors": false,
    "authorization_code": {
      "uri": "/oauth2/token",
      "client_id": "fe5dadd9-34ad-430f-a8f7-c75e81cc5d7b",
      "client_secret": "GXkHy-1aSPyo4pst8WBWbg",
      "redirect_uri": "https://localhost:8080/authorization_code",
      "scope": [
        "api_b.r",
        "api_b_w",
        "api_c.r",
        "api_c_w"
      ]
    },
    "client_credentials": {
      "uri": "/oauth2/token",
      "client_id": "fe5dadd9-34ad-430f-a8f7-c75e81cc5d7b",
      "client_secret": "GXkHy-1aSPyo4pst8WBWbg",
      "scope": [
        "api_b.r",
        "api_b_w",
        "api_c.r",
        "api_c_w"
      ]
    }
  }
}
```

And this is what looks like in API B client.json

```
{
  "description": "client configuration, all timing is milli-second",
  "sync": {
    "maxConnectionTotal": 100,
    "maxConnectionPerRoute": 10,
    "routes": {
      "api.google.com": 20,
      "api.facebook.com": 10
    },
    "timeout": 10000,
    "keepAlive": 15000
  },
  "async": {
    "maxConnectionTotal": 100,
    "maxConnectionPerRoute": 10,
    "routes": {
      "api.google.com": 20,
      "api.facebook.com": 10
    },
    "reactor": {
      "ioThreadCount": 1,
      "connectTimeout": 10000,
      "soTimeout": 10000
    },
    "timeout": 10000,
    "keepAlive": 15000
  },
  "tls": {
    "verifyHostname": false,
    "loadTrustStore": false,
    "trustStore": "trust.keystore",
    "trustPass": "password",
    "loadKeyStore": false,
    "keyStore": "key.jks",
    "keyPass": "password"
  },
  "oauth": {
    "tokenRenewBeforeExpired": 600000,
    "expiredRefreshRetryDelay": 5000,
    "earlyRefreshRetryDelay": 30000,
    "server_url": "http://localhost:8888",
    "timeout": 5000,
    "ignoreSSLErrors": false,
    "authorization_code": {
      "uri": "/oauth2/token",
      "client_id": "10a0a743-4674-4a9d-8867-db63ad4c8b4e",
      "client_secret": "tcahI1dvT1OsxXxg-IB_-w",
      "redirect_uri": "https://localhost:8080/authorization_code",
      "scope": [
        "api_d.r",
        "api_d_w"
      ]
    },
    "client_credentials": {
      "uri": "/oauth2/token",
      "client_id": "10a0a743-4674-4a9d-8867-db63ad4c8b4e",
      "client_secret": "tcahI1dvT1OsxXxg-IB_-w",
      "scope": [
        "api_d.r",
        "api_d_w"
      ]
    }
  }
}

```

With client.json updated in both API A and API B, we need to update the code
to assign scope token during runtime.

Open API A PathHandlerProvider and three lines.

```
package io.swagger.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.client.Client;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import com.networknt.server.HandlerProvider;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Methods;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class PathHandlerProvider implements HandlerProvider {

    public HttpHandler getHandler() {
        HttpHandler handler = Handlers.routing()


            .add(Methods.GET, "/v1/data", new HttpHandler() {
                        public void handleRequest(HttpServerExchange exchange) throws Exception {
                            List<String> list = new ArrayList<String>();
                            // get passed in Authorization header
                            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                            final HttpGet[] requests = new HttpGet[] {
                                    new HttpGet("http://localhost:8081/v1/data"),
                                    new HttpGet("http://localhost:8082/v1/data"),
                            };
                            try {
                                CloseableHttpAsyncClient client = Client.getInstance().getAsyncClient();
                                final CountDownLatch latch = new CountDownLatch(requests.length);
                                for (final HttpGet request: requests) {
                                    Client.getInstance().addAuthorizationWithScopeToken(request, authHeader);
                                    client.execute(request, new FutureCallback<HttpResponse>() {
                                        @Override
                                        public void completed(final HttpResponse response) {
                                            latch.countDown();
                                            try {
                                                List<String> apiList = (List<String>) Config.getInstance().getMapper().readValue(response.getEntity().getContent(),
                                                        new TypeReference<List<String>>(){});
                                                list.addAll(apiList);
                                            } catch (IOException e) {

                                            }
                                        }

                                        @Override
                                        public void failed(final Exception ex) {
                                            latch.countDown();
                                        }

                                        @Override
                                        public void cancelled() {
                                            latch.countDown();
                                        }
                                    });
                                }
                                latch.await();
                            } catch (ClientException e) {
                                throw new Exception("ClientException:", e);
                            }
                            // now add API A specific messages
                            list.add("API A: Message 1");
                            list.add("API A: Message 2");
                            exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(list));
                        }
                    })

        ;
        return handler;
    }
}

```

Open API B PathHandlerProvider and add three lines

```
package io.swagger.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.client.Client;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import com.networknt.server.HandlerProvider;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Methods;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PathHandlerProvider implements HandlerProvider {
    static final Logger logger = LoggerFactory.getLogger(PathHandlerProvider.class);

    String apidUrl = "http://localhost:8083/v1/data";
    public HttpHandler getHandler() {
        HttpHandler handler = Handlers.routing()


            .add(Methods.GET, "/v1/data", new HttpHandler() {
                        public void handleRequest(HttpServerExchange exchange) throws Exception {
                            // get passed in Authorization header
                            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

                            List<String> list = new ArrayList<String>();
                            try {
                                CloseableHttpClient client = Client.getInstance().getSyncClient();
                                HttpGet httpGet = new HttpGet(apidUrl);
                                Client.getInstance().addAuthorizationWithScopeToken(httpGet, authHeader);
                                CloseableHttpResponse response = client.execute(httpGet);
                                int responseCode = response.getStatusLine().getStatusCode();
                                if(responseCode != 200){
                                    throw new Exception("Failed to call API D: " + responseCode);
                                }
                                List<String> apidList = (List<String>) Config.getInstance().getMapper().readValue(response.getEntity().getContent(),
                                        new TypeReference<List<String>>(){});
                                list.addAll(apidList);
                            } catch (ClientException e) {
                                    throw new Exception("Client Exception: ", e);
                            } catch (IOException e) {
                                    throw new Exception("IOException:", e);
                            }
                            // now add API B specific messages
                            list.add("API B: Message 1");
                            list.add("API B: Message 2");
                            exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(list));
                        }
                    })

        ;
        return handler;
    }
}


```

Let's start these servers and test it. Don't forget to start undertow-oauth2
server.

Run the following command.

```
curl -H "Authorization: Bearer eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NidvcmtudCIsImV4cCI6MTc5MDAzNTcwOSwianRpIjoiSTJnSmdBSHN6NzJEV2JWdUFMdUU2QSIsImlhdCI6MTQ3NDY3NTcwOSwibmJmIjoxNDc0Njc1NTg5LCJ2ZXJzaW9uIjoiMS4wIiwidXNlcl9pZCI6InN0ZXZlIiwidXNlcl90eXBlIjoiRU1QTE9ZRUUiLCJjbGllbnRfaWQiOiJmN2Q0MjM0OC1jNjQ3LTRlZmItYTUyZC00YzU3ODc0MjFlNzIiLCJzY29wZSI6WyJ3cml0ZTpwZXRzIiwicmVhZDpwZXRzIl19.mue6eh70kGS3Nt2BCYz7ViqwO7lh_4JSFwcHYdJMY6VfgKTHhsIGKq2uEDt3zwT56JFAePwAxENMGUTGvgceVneQzyfQsJeVGbqw55E9IfM_uSM-YcHwTfR7eSLExN4pbqzVDI353sSOvXxA98ZtJlUZKgXNE1Ngun3XFORCRIB_eH8B0FY_nT_D1Dq2WJrR-re-fbR6_va95vwoUdCofLRa4IpDfXXx19ZlAtfiVO44nw6CS8O87eGfAm7rCMZIzkWlCOFWjNHnCeRsh7CVdEH34LF-B48beiG5lM7h4N12-EME8_VDefgMjZ8eqs1ICvJMxdIut58oYbdnkwTjkA" localhost:8080/v1/data
```

And here is the result.

```
["API C: Message 1","API C: Message 2","API D: Message 1","API D: Message 2","API B: Message 1","API B: Message 2","API A: Message 1","API A: Message 2"]
```
## Dockerization

## Integration

## Performance

## Production

## Conclusion

