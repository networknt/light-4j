---
date: 2016-10-09T08:01:56-04:00
title: Chain Pattern Microservices
---

## Introduction

These days light weight container like Docker is getting traction, more and more 
API services are developed for docker container and deployed to the cloud. In this
environment, traditional heavy weight containers like Java EE and Spring are 
losing ground as it doesn't make sense to have a heavy weight container wrapped 
with a light weight docker container. Docker and container orchestration tools 
like Kubernetes and Docker Swarm are replacing all the functionalities Java EE 
provides without hogging resources.

There is an [article](https://www.gartner.com/doc/reprints?id=1-3N8E378&ct=161205&st=sb) 
published by Gartner indicates that both Java EE and .NET are declining and will
be replaced very soon. 


Another clear trend is standalone Gateway is phasing out in the cloud environment 
with docker containers as most of the traditional gateway features are replaced 
by container orchestration tool and docker container management tools. In addition, 
some of the cross cutting concerns gateway provided are addressed in API frameworks
like Light-4J.


This tutorial shows you how to build 4 services chained together one by one. And it will
be the foundation for our microserives benchmarks.

API A -> API B -> API C -> API D

## Prepare workspace

All specifications and code of the services are on github.com but we are going to
redo it again by following the steps in the tutorial. Let's first create a
workspace. I have created a directory named networknt under user directory.

Checkout related projects.

```
cd ~/networknt
git clone git@github.com:networknt/swagger-codegen.git
git clone git@github.com:networknt/light-java-example.git
git clone git@github.com:networknt/swagger.git
git clone git@github.com:networknt/light-oauth2.git
git clone git@github.com:networknt/light-docker.git

```

As we are going to regenerate API A, B, C and D, let's rename ms_chain folder from
light-example-4j.

```
cd ~/networknt/light-java-example
mv ms_chain ms_chain.bak
cd ..
```

## Specifications

Light Java Microservices Framework encourages Design Driven API building and 
[OpenAPI Specification](https://github.com/OAI/OpenAPI-Specification) is the central
piece to drive the runtime for security and validation. Also, the specification 
can be used to scaffold a running server project the first time so that developers 
can focus their efforts on the domain business logic implementation without 
worrying about how each component wired together.

During the service implementation phase, specification might be changed and you can
regenerate the service codebase again without overwriting your handlers and test
cases for handlers. 

To create swagger specification, the best tool is
[swagger-editor](http://swagger.io/swagger-editor/) and I have an
[article](https://networknt.github.io/light-4j/tools/swagger-editor/)
in tools section to describe how to use it.

By following the [instructions](https://networknt.github.io/light-4j/tools/swagger-editor/)
on how to use the editor, let's create four API specifications in swagger repo.

API A will call API B, API B will call API C, API C will call API D

```
API A -> API B -> API C -> API D
```

Here is the API A swagger.yaml and others can be found at
[https://github.com/networknt/swagger](https://github.com/networknt/swagger) or swagger
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
to start four projects in light-example-4j/ms_chain. In normal API build, you 
should create a repo for each API.

#### Build Light Java Generator

As [swagger-codegen](https://github.com/swagger-api/swagger-codegen) doesn't
support Java 8, I have forked it [here](https://github.com/networknt/swagger-codegen).

The project is cloned to the local already during the prepare stage. Let's build it.

```
cd swagger-codegen
mvn clean install -DskipTests
```

#### Generate first project

Now you have your swagger-codegen built, let's generate a project. Assume that
swagger, light-example-4j and swagger-codegen are in the same working
directory ~/networknt and you are in ~/networknt/swagger-codegen now.

```
java -jar modules/swagger-codegen-cli/target/swagger-codegen-cli.jar generate -i ../swagger/api_a/swagger.yaml -l light-java -o ../light-java-example/ms_chain/api_a/generated

```
Here is the generator output.

```
[main] INFO io.swagger.parser.Swagger20Parser - reading from ../swagger/api_a/swagger.yaml
[main] WARN io.swagger.codegen.DefaultCodegen - Empty operationId found for path: get /server/info. Renamed to auto-generated operationId: serverInfoGet
[main] INFO io.swagger.codegen.AbstractGenerator - writing file /Users/stevehu/networknt/swagger-codegen/../light-java-example/ms_chain/api_a/generated/src/main/java/io/swagger/handler/DataGetHandler.java
[main] INFO io.swagger.codegen.AbstractGenerator - writing file /Users/stevehu/networknt/swagger-codegen/../light-java-example/ms_chain/api_a/generated/src/test/java/io/swagger/handler/DataGetHandlerTest.java
[main] INFO io.swagger.codegen.AbstractGenerator - writing file /Users/stevehu/networknt/swagger-codegen/../light-java-example/ms_chain/api_a/generated/src/main/java/io/swagger/handler/ServerInfoGetHandler.java
[main] INFO io.swagger.codegen.AbstractGenerator - writing file /Users/stevehu/networknt/swagger-codegen/../light-java-example/ms_chain/api_a/generated/src/test/java/io/swagger/handler/ServerInfoGetHandlerTest.java
swaggerio.swagger.models.Swagger@dc2e0c3c
[main] INFO io.swagger.codegen.AbstractGenerator - writing file /Users/stevehu/networknt/swagger-codegen/../light-java-example/ms_chain/api_a/generated/pom.xml
[main] INFO io.swagger.codegen.AbstractGenerator - writing file /Users/stevehu/networknt/swagger-codegen/../light-java-example/ms_chain/api_a/generated/README.md
[main] INFO io.swagger.codegen.AbstractGenerator - writing file /Users/stevehu/networknt/swagger-codegen/../light-java-example/ms_chain/api_a/generated/.gitignore
[main] INFO io.swagger.codegen.AbstractGenerator - writing file /Users/stevehu/networknt/swagger-codegen/../light-java-example/ms_chain/api_a/generated/Dockerfile
[main] INFO io.swagger.codegen.AbstractGenerator - writing file /Users/stevehu/networknt/swagger-codegen/../light-java-example/ms_chain/api_a/generated/.classpath
[main] INFO io.swagger.codegen.AbstractGenerator - writing file /Users/stevehu/networknt/swagger-codegen/../light-java-example/ms_chain/api_a/generated/.project
[main] INFO io.swagger.codegen.AbstractGenerator - writing file /Users/stevehu/networknt/swagger-codegen/../light-java-example/ms_chain/api_a/generated/src/main/resources/config/swagger.json
[main] INFO io.swagger.codegen.AbstractGenerator - writing file /Users/stevehu/networknt/swagger-codegen/../light-java-example/ms_chain/api_a/generated/src/main/java/io/swagger/PathHandlerProvider.java
[main] INFO io.swagger.codegen.AbstractGenerator - writing file /Users/stevehu/networknt/swagger-codegen/../light-java-example/ms_chain/api_a/generated/src/test/java/io/swagger/handler/TestServer.java
[main] INFO io.swagger.codegen.AbstractGenerator - writing file /Users/stevehu/networknt/swagger-codegen/../light-java-example/ms_chain/api_a/generated/src/main/resources/META-INF/services/com.networknt.server.HandlerProvider
[main] INFO io.swagger.codegen.AbstractGenerator - writing file /Users/stevehu/networknt/swagger-codegen/../light-java-example/ms_chain/api_a/generated/src/main/resources/META-INF/services/com.networknt.handler.MiddlewareHandler
[main] INFO io.swagger.codegen.AbstractGenerator - writing file /Users/stevehu/networknt/swagger-codegen/../light-java-example/ms_chain/api_a/generated/src/main/resources/META-INF/services/com.networknt.server.StartupHookProvider
[main] INFO io.swagger.codegen.AbstractGenerator - writing file /Users/stevehu/networknt/swagger-codegen/../light-java-example/ms_chain/api_a/generated/src/main/resources/META-INF/services/com.networknt.server.ShutdownHookProvider
[main] INFO io.swagger.codegen.AbstractGenerator - writing file /Users/stevehu/networknt/swagger-codegen/../light-java-example/ms_chain/api_a/generated/src/main/resources/config/server.yml
[main] INFO io.swagger.codegen.AbstractGenerator - writing file /Users/stevehu/networknt/swagger-codegen/../light-java-example/ms_chain/api_a/generated/src/main/resources/config/security.yml
[main] INFO io.swagger.codegen.AbstractGenerator - writing file /Users/stevehu/networknt/swagger-codegen/../light-java-example/ms_chain/api_a/generated/src/main/resources/config/secret.yml
[main] INFO io.swagger.codegen.DefaultGenerator - writing file /Users/stevehu/networknt/swagger-codegen/../light-java-example/ms_chain/api_a/generated/src/main/resources/config/oauth/primary.crt
[main] INFO io.swagger.codegen.DefaultGenerator - writing file /Users/stevehu/networknt/swagger-codegen/../light-java-example/ms_chain/api_a/generated/src/main/resources/config/oauth/secondary.crt
[main] INFO io.swagger.codegen.DefaultGenerator - writing file /Users/stevehu/networknt/swagger-codegen/../light-java-example/ms_chain/api_a/generated/src/main/resources/config/tls/server.keystore
[main] INFO io.swagger.codegen.DefaultGenerator - writing file /Users/stevehu/networknt/swagger-codegen/../light-java-example/ms_chain/api_a/generated/src/main/resources/config/tls/server.truststore
[main] INFO io.swagger.codegen.DefaultGenerator - writing file /Users/stevehu/networknt/swagger-codegen/../light-java-example/ms_chain/api_a/generated/src/main/resources/logback.xml
[main] INFO io.swagger.codegen.DefaultGenerator - writing file /Users/stevehu/networknt/swagger-codegen/../light-java-example/ms_chain/api_a/generated/src/test/resources/logback-test.xml
[main] INFO io.swagger.codegen.AbstractGenerator - writing file /Users/stevehu/networknt/swagger-codegen/../light-java-example/ms_chain/api_a/generated/.swagger-codegen-ignore
[main] INFO io.swagger.codegen.AbstractGenerator - writing file /Users/stevehu/networknt/swagger-codegen/../light-java-example/ms_chain/api_a/generated/LICENSE

```

#### Build and run the mock API

And now you have a new project created in light-example-4j. Let's build
it and run the test cases. If everything is OK, start the server.

```
cd ..
cd light-java-example/ms_chain/api_a/generated
mvn clean install
mvn exec:exec
```

Let's test the API A by issuing the following command
```
curl localhost:8080/v1/data
```

By default the generated response example will be returned. 

```
[ "aeiou" ]
```

#### Generate other APIs

Let's kill the API A by Ctrl+C and move to the swagger-codegen folder again. Follow 
the above steps to generate other APIs. Make sure you are in swagger_codegen
directory.

```
java -jar modules/swagger-codegen-cli/target/swagger-codegen-cli.jar generate -i ../swagger/api_b/swagger.yaml -l light-java -o ../light-java-example/ms_chain/api_b/generated
java -jar modules/swagger-codegen-cli/target/swagger-codegen-cli.jar generate -i ../swagger/api_c/swagger.yaml -l light-java -o ../light-java-example/ms_chain/api_c/generated
java -jar modules/swagger-codegen-cli/target/swagger-codegen-cli.jar generate -i ../swagger/api_d/swagger.yaml -l light-java -o ../light-java-example/ms_chain/api_d/generated

```

Now you have four APIs generated from four OpenAPI specifications. Let's check
them in. Note that you might not have write access to this repo, so you can ignore
this step. 

```
cd ../light-java-example
git add .
git commit -m "checkin 4 apis"
git push origin master
```

## ApiToApi

Now these APIs are working if you start them and they will output the mock responses
generated based on the API specifications. But you have to start them one by one as they
are all binding to the same port at localhost. Let's take a look at the API handler itself
and update it based on our business logic and update the configuration to start them at
the same time. Once they are up and running, you can call API A and subsequently all other
APIs will be called in a chain.

#### Prepare Environment

Before starting this step, let's create a folder called apitoapi in each sub folder under
ms_chain and copy everything from generated folder to the apitoapi. We are going to update
apitoapi folder to have business logic to call another api and change the configuration
to listen to different port. You can compare between generated and apitoapi to see what has
been changed later on.

```
cd ~/networknt/light-java-example/ms_chain/api_a
cp -r generated apitoapi
cd ~/networknt/light-java-example/ms_chain/api_b
cp -r generated apitoapi
cd ~/networknt/light-java-example/ms_chain/api_b
cp -r generated apitoapi
cd ~/networknt/light-java-example/ms_chain/api_d
cp -r generated apitoapi

```

Now we have apitoapi folder copied from generated and all updates in this step will be
in apitoapi folder. 

#### API D
Let's take a look at the PathHandlerProvider.java in
ms_chain/api_d/apitoapi/src/main/java/io/swagger/

```
package io.swagger;

import com.networknt.config.Config;
import com.networknt.server.HandlerProvider;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Methods;
import com.networknt.info.ServerInfoGetHandler;
import io.swagger.handler.*;

public class PathHandlerProvider implements HandlerProvider {
    @Override
    public HttpHandler getHandler() {
        return Handlers.routing()
            .add(Methods.GET, "/v1/data", new DataGetHandler())
            .add(Methods.GET, "/v1/server/info", new ServerInfoGetHandler())
        ;
    }
}
```

This is the only class that routes each endpoint defined in specification to a handler 
instance. Because we only have one endpoint /v1/data@get there is only one route added
to the handler chain. And there is a handler generated in the handler subfolder to
handle request that has the url matched to this endpoint. The /server/info is injected 
to output the server runtime information on all the components and configurations. It
will be included in every API/service.

The generated handler is named "DataGetHandler" and it returns example response generated
based on the swagger specification. Here is the generated handler code. 
 
```
package io.swagger.handler;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringEscapeUtils;

public class DataGetHandler implements HttpHandler {
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Map<String, Object> examples = new HashMap<>();
        examples.put("application/json", StringEscapeUtils.unescapeHtml4("[ &quot;aeiou&quot; ]"));
        if(examples.size() > 0) {
            exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
            exchange.getResponseSender().send((String)examples.get("application/json"));
        } else {
            exchange.endExchange();
        }
    }
}
```

Let's update it to an array of strings that indicates the response comes from API D. 


```
package io.swagger.handler;

import com.networknt.config.Config;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringEscapeUtils;

public class DataGetHandler implements HttpHandler {

    public void handleRequest(HttpServerExchange exchange) throws Exception {
        List<String> messages = new ArrayList<String>();
        messages.add("API D: Message 1");
        messages.add("API D: Message 2");
        exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(messages));
    }
}
```

In order to start all servers at the same time, let's update server.yml to user
port 7004 instead of default 8080 for http and 7444 for https.

The server.yml is located at
~/networknt/light-example-4j/ms_chain/api_d/apitoapi/src/main/resources/config

```
# Server configuration
---
# This is the default binding address if the service is dockerized.
ip: 0.0.0.0

# Http port if enableHttp is true.
httpPort: 7004

# Enable HTTP should be false on official environment.
enableHttp: true

# Https port if enableHttps is true.
httpsPort: 7444

# Enable HTTPS should be true on official environment.
enableHttps: true

# Keystore file name in config folder. KeystorePass is in secret.yml to access it.
keystoreName: tls/server.keystore

# Flag that indicate if two way TLS is enabled. Not recommended in docker container.
enableTwoWayTls: false

# Truststore file name in config folder. TruststorePass is in secret.yml to access it.
truststoreName: tls/server.truststore

# Unique service identifier. Used in service registration and discovery etc.
serviceId: io.swagger.swagger-light-java-1.0.0

# Flag to enable service registration. Only be true if running as standalone Java jar.
enableRegistry: false

```


Now, let's build it and start the server. 
```
cd ~/networknt/light-java-example/ms_chain/api_d/apitoapi
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

Test with HTTPS port and you should have the same result.

```
curl -k https://localhost:7444/v1/data
```

Note that we have -k option to in the https command line as we are using self-signed
certificate and we don't want to verify the domain.


#### API C
Let's leave API D running and update API C DataGetHandler in 
~/networknt/light-example-4j/ms_chain/api_c/apitoapi


```
package io.swagger.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.client.Client;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

public class DataGetHandler implements HttpHandler {

    static String CONFIG_NAME = "api_c";
    static String apidUrl = (String)Config.getInstance().getJsonMapConfig(CONFIG_NAME).get("api_d_endpoint");

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
        list.add("API C: Message 1");
        list.add("API C: Message 2");
        exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(list));
    }
}

```

Now let's change the server.yml to have http port 7003 and https port 7443

```
# Server configuration
---
# This is the default binding address if the service is dockerized.
ip: 0.0.0.0

# Http port if enableHttp is true.
httpPort: 7003

# Enable HTTP should be false on official environment.
enableHttp: true

# Https port if enableHttps is true.
httpsPort: 7443

# Enable HTTPS should be true on official environment.
enableHttps: true

# Keystore file name in config folder. KeystorePass is in secret.yml to access it.
keystoreName: tls/server.keystore

# Flag that indicate if two way TLS is enabled. Not recommended in docker container.
enableTwoWayTls: false

# Truststore file name in config folder. TruststorePass is in secret.yml to access it.
truststoreName: tls/server.truststore

# Unique service identifier. Used in service registration and discovery etc.
serviceId: io.swagger.swagger-light-java-1.0.0

# Flag to enable service registration. Only be true if running as standalone Java jar.
enableRegistry: false

```

API C needs to have the url of API D in order to call it. Let's put it in a config file for
now and move to service discovery later.

Create api_c.yml in src/main/resources/config folder.

```
api_d_endpoint: "http://localhost:7004/v1/data"

```


Start API C server and test the endpoint /v1/data

```
cd ~/networknt/light-java-example/ms_chain/api_c/apitoapi
mvn clean install exec:exec
```
From another terminal window run:

```
curl localhost:7003/v1/data
```
And the result is

```
["API D: Message 1","API D: Message 2","API C: Message 1","API C: Message 2"]
```

#### API B

Let's keep API C and API D running. The next step is to complete API B. API B 
will call API C to fulfill its request. 

Now let's update the generated DataGetHandler.java to this.

```
package io.swagger.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.client.Client;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

public class DataGetHandler implements HttpHandler {
    static String CONFIG_NAME = "api_b";
    static String apidUrl = (String)Config.getInstance().getJsonMapConfig(CONFIG_NAME).get("api_c_endpoint");

    public void handleRequest(HttpServerExchange exchange) throws Exception {
        List<String> list = new ArrayList<String>();
        try {
            CloseableHttpClient client = Client.getInstance().getSyncClient();
            HttpGet httpGet = new HttpGet(apidUrl);
            CloseableHttpResponse response = client.execute(httpGet);
            int responseCode = response.getStatusLine().getStatusCode();
            if(responseCode != 200){
                throw new Exception("Failed to call API C: " + responseCode);
            }
            List<String> apicList = (List<String>) Config.getInstance().getMapper().readValue(response.getEntity().getContent(),
                    new TypeReference<List<String>>(){});
            list.addAll(apicList);
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
}
```

Now let's change the server.yml to have http port 7002 and https port 7442

```
# Server configuration
---
# This is the default binding address if the service is dockerized.
ip: 0.0.0.0

# Http port if enableHttp is true.
httpPort: 7002

# Enable HTTP should be false on official environment.
enableHttp: true

# Https port if enableHttps is true.
httpsPort: 7442

# Enable HTTPS should be true on official environment.
enableHttps: true

# Keystore file name in config folder. KeystorePass is in secret.yml to access it.
keystoreName: tls/server.keystore

# Flag that indicate if two way TLS is enabled. Not recommended in docker container.
enableTwoWayTls: false

# Truststore file name in config folder. TruststorePass is in secret.yml to access it.
truststoreName: tls/server.truststore

# Unique service identifier. Used in service registration and discovery etc.
serviceId: io.swagger.swagger-light-java-1.0.0

# Flag to enable service registration. Only be true if running as standalone Java jar.
enableRegistry: false

```

API B needs to have the url of API C in order to call it. Let's put it in a config file for
now and move to service discovery later.

Create api_b.yml in src/main/resources/config folder.

```
api_c_endpoint: "http://localhost:7003/v1/data"

```


Start API B server and test the endpoint /v1/data

```
cd ~/networknt/light-java-example/ms_chain/api_b/apitoapi
mvn clean install exec:exec
```
From another terminal window run:

```
curl localhost:7002/v1/data
```
And the result is

```
["API D: Message 1","API D: Message 2","API C: Message 1","API C: Message 2","API B: Message 1","API B: Message 2"]
```


#### API A

API A will call API B to fulfill its request. Now let's update the 
generated DataGetHandler.java code to

```
package io.swagger.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.client.Client;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

public class DataGetHandler implements HttpHandler {
    static String CONFIG_NAME = "api_a";
    static String apidUrl = (String)Config.getInstance().getJsonMapConfig(CONFIG_NAME).get("api_b_endpoint");

    public void handleRequest(HttpServerExchange exchange) throws Exception {
        List<String> list = new ArrayList<String>();
        try {
            CloseableHttpClient client = Client.getInstance().getSyncClient();
            HttpGet httpGet = new HttpGet(apidUrl);
            CloseableHttpResponse response = client.execute(httpGet);
            int responseCode = response.getStatusLine().getStatusCode();
            if(responseCode != 200){
                throw new Exception("Failed to call API B: " + responseCode);
            }
            List<String> apicList = (List<String>) Config.getInstance().getMapper().readValue(response.getEntity().getContent(),
                    new TypeReference<List<String>>(){});
            list.addAll(apicList);
        } catch (ClientException e) {
            throw new Exception("Client Exception: ", e);
        } catch (IOException e) {
            throw new Exception("IOException:", e);
        }
        // now add API B specific messages
        list.add("API A: Message 1");
        list.add("API A: Message 2");
        exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(list));
    }
}

```

Now let's change the server.yml to have http port 7001 and https port 7441

```
# Server configuration
---
# This is the default binding address if the service is dockerized.
ip: 0.0.0.0

# Http port if enableHttp is true.
httpPort: 7001

# Enable HTTP should be false on official environment.
enableHttp: true

# Https port if enableHttps is true.
httpsPort: 7441

# Enable HTTPS should be true on official environment.
enableHttps: true

# Keystore file name in config folder. KeystorePass is in secret.yml to access it.
keystoreName: tls/server.keystore

# Flag that indicate if two way TLS is enabled. Not recommended in docker container.
enableTwoWayTls: false

# Truststore file name in config folder. TruststorePass is in secret.yml to access it.
truststoreName: tls/server.truststore

# Unique service identifier. Used in service registration and discovery etc.
serviceId: io.swagger.swagger-light-java-1.0.0

# Flag to enable service registration. Only be true if running as standalone Java jar.
enableRegistry: false

```

API A needs to have the url of API in order to call it. Let's put it in a config file for
now and move to service discovery later.

Create api_a.yml in src/main/resources/config folder.

```
api_b_endpoint: "http://localhost:7002/v1/data"

```


Start API A server and test the endpoint /v1/data

```
cd ~/networknt/light-java-example/ms_chain/api_a/apitoapi
mvn clean install exec:exec
```
From another terminal window run:

```
curl localhost:7002/v1/data
```
And the result is

```
["API D: Message 1","API D: Message 2","API C: Message 1","API C: Message 2","API B: Message 1","API B: Message 2","API A: Message 1","API A: Message 2"]
```


At this moment, we have all four APIs completed and A is calling B, B is calling C and
C is calling D. 

## Performance without security

Now let's see if these servers are performing with
[wrk](https://github.com/wg/wrk). To learn how to use it, please see my
article in tools [here](https://networknt.github.io/light-4j/tools/wrk-perf/)

Assume you have wrk installed, run the following command.

```
wrk -t4 -c128 -d30s http://localhost:7001 -s pipeline.lua --latency -- /v1/data 1024

```
And here is what I got on my laptop

```
wrk -t4 -c128 -d30s http://localhost:7001 -s pipeline.lua --latency -- /v1/data 1024
Running 30s test @ http://localhost:7001
  4 threads and 128 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     0.00us    0.00us   0.00us     nan%
    Req/Sec     2.54k     1.32k    9.52k    71.22%
  Latency Distribution
     50%    0.00us
     75%    0.00us
     90%    0.00us
     99%    0.00us
  286072 requests in 30.09s, 65.75MB read
  Socket errors: connect 0, read 0, write 0, timeout 254
Requests/sec:   9505.92
Transfer/sec:      2.18MB
```

Before starting the next step, please kill all four instances by Ctrl+C.

## Docker Compose

So far, we have four servers running and they can talk to each other as standalone
services. In this step, we are going to dockerize all of them and start them with a
docker-compose up command. 

Before doing that, let's create two folders under ms_chain for different version of
compose files and externalized config files.

```
cd ~/networknt/light-java-example/ms_chain
mkdir compose
mkdir config
cd compose
mkdir apionly
```

Now in the config folder, we need to create sub folders for each api and inside, we need
to create apionly folder.

```
cd ~/networknt/light-java-example/ms_chain/config
mkdir api_a
mkdir api_b
mkdir api_c
mkdir api_d

cd api_a
mkdir apionly
cd ../api_b
mkdir apionly
cd ../api_c
mkdir apionly
cd ../api_d
mkdir apionly

```


Let's create a ms_chain/compose/apionly/docker-compose-app.yml.

```
#
# docker-compose-app.yml
#

version: '2'

#
# Services
#
services:


    #
    # Microservice: API A
    #
    apia:
        build: ~/networknt/light-java-example/ms_chain/api_a/apitoapi/
        ports:
            - "7001:7001"
        networks:
            - localnet
        volumes:
            - ~/networknt/light-java-example/ms_chain/config/api_a/apionly:/config

    #
    # Microservice: API B
    #
    apib:
        build: ~/networknt/light-java-example/ms_chain/api_b/apitoapi/
        ports:
            - "7002:7002"
        networks:
            - localnet
        volumes:
            - ~/networknt/light-java-example/ms_chain/config/api_b/apionly:/config

    #
    # Microservice: API C
    #
    apic:
        build: ~/networknt/light-java-example/ms_chain/api_c/apitoapi/
        ports:
            - "7003:7003"
        networks:
            - localnet
        volumes:
            - ~/networknt/light-java-example/ms_chain/config/api_c/apionly:/config

    #
    # Microservice: API D
    #
    apid:
        build: ~/networknt/light-java-example/ms_chain/api_d/apitoapi/
        ports:
            - "7004:7004"
        networks:
            - localnet
        volumes:
            - ~/networknt/light-java-example/ms_chain/config/api_d/apionly:/config

#
# Networks
#
networks:
    localnet:
        external: true
```


From above docker compose file you can see we have a volume for each api with externalized
configuration folder under ms_chain/config/api_x/apionly. Since we are using docker compose
we cannot use localhost:port to call services and we have to use service name in 
docker-compose-app.yml for the hostname. To resolve the issue without rebuilding the services
we are going to externalize api_a.yml, api_b.yml and api_c.yml to their externalized config
folder.

Let's create api_a.yml in ms_chain/config/api_a/apionly folder

```
api_b_endpoint: "http://apib:7002/v1/data"

```
Please note that apib is the name of the service in docker-compose-app.yml

Create api_b.yml in ms_chain/config/api_b/apionly folder

```
api_c_endpoint: "http://apic:7003/v1/data"

```

Create api_c.yml in ms_chain/config/api_c/apionly folder

```
api_d_endpoint: "http://apid:7004/v1/data"

```


Now let's start the docker compose. 

```
cd ~/networknt/light-java-example/ms_chain/compose/apionly
docker-compose -f docker-compose-app.yml up
```

Let's test if the servers are working.

```
curl localhost:7001/v1/data
```

And we will have the result.

```
["API D: Message 1","API D: Message 2","API C: Message 1","API C: Message 2","API B: Message 1","API B: Message 2","API A: Message 1","API A: Message 2"]
```

Let's run a load test now.

```
wrk -t4 -c128 -d30s http://localhost:7001 -s pipeline.lua --latency -- /v1/data 1024
Running 30s test @ http://localhost:7001
  4 threads and 128 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     0.00us    0.00us   0.00us     nan%
    Req/Sec   592.29    423.59     2.72k    70.98%
  Latency Distribution
     50%    0.00us
     75%    0.00us
     90%    0.00us
     99%    0.00us
  39712 requests in 30.10s, 9.13MB read
  Socket errors: connect 0, read 4, write 5, timeout 0
Requests/sec:   1319.39
Transfer/sec:    310.52KB
```


## Enable OAuth2 Security

So far, we've started four servers and tested them successfully; however,
these servers are not protected by OAuth2 JWT tokens as it is turned off
by default in the generated code.

Before we turn on the security, we need to have [light-oauth2](https://github.com/networknt/light-oauth2)
server up and running so that these servers can get JWT token in real time.

When we enable security, the source code needs to be updated in order to
leverage client module to get JWT token automatically. Let's prepare the
environment. 

#### Update  APIs

Since we are going to change the code, let's copy each service into a new folder
called security from apitoapi. 

```
cd ~/networknt/light-java-example/ms_chain/api_a
cp -r apitoapi security
cd ~/networknt/light-java-example/ms_chain/api_b
cp -r apitoapi security
cd ~/networknt/light-java-example/ms_chain/api_c
cp -r apitoapi security
cd ~/networknt/light-java-example/ms_chain/api_d
cp -r apitoapi security

```
Now for api_a, api_b and api_c we need to update DataGetHandler.java to add
a line before client.execute.

```
            Client.getInstance().propagateHeaders(httpGet, exchange);

```

Here is the updated file for api_a

```
package io.swagger.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.client.Client;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

public class DataGetHandler implements HttpHandler {
    static String CONFIG_NAME = "api_a";
    static String apibUrl = (String)Config.getInstance().getJsonMapConfig(CONFIG_NAME).get("api_b_endpoint");

    public void handleRequest(HttpServerExchange exchange) throws Exception {
        List<String> list = new ArrayList<String>();
        try {
            CloseableHttpClient client = Client.getInstance().getSyncClient();
            HttpGet httpGet = new HttpGet(apibUrl);
            Client.getInstance().propagateHeaders(httpGet, exchange);
            CloseableHttpResponse response = client.execute(httpGet);
            int responseCode = response.getStatusLine().getStatusCode();
            if(responseCode != 200){
                throw new Exception("Failed to call API B: " + responseCode);
            }
            List<String> apicList = (List<String>) Config.getInstance().getMapper().readValue(response.getEntity().getContent(),
                    new TypeReference<List<String>>(){});
            list.addAll(apicList);
        } catch (ClientException e) {
            throw new Exception("Client Exception: ", e);
        } catch (IOException e) {
            throw new Exception("IOException:", e);
        }
        // now add API B specific messages
        list.add("API A: Message 1");
        list.add("API A: Message 2");
        exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(list));
    }
}

```

Follow api_a, update api_b and api_c.



#### Update Config

Now let's update security.yml to enable JWT verification and scope verification
for each service. This file is located at src/main/resources/config folder.

API A

old file

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

Update to 

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

Update the security.yml for api_b, api_c and api_d in security folder.


#### Start OAuth2 Services


The easiest way to run light-oauth2 services is through docker-compose. In the preparation
step, we have cloned light-docker repo. 

Let's start the light-oauth2 services from a docker compose.

```
cd ~/networknt/light-docker
docker-compose -f docker-compose-oauth2-mysql.yml up
```
Now the OAuth2 services are up and running. 

#### Register Client

Before we start integrate with OAuth2 services, we need to register clients for api_a,
api_b, api_c and api_d. This step should be done from light-portal for official environment.
After client registration, we need to remember the client_id and client_secret for each in
order to update client.yml for each service.

For more details on how to use the command line tool or script to access oauth2 services,
please see this [tutorial](https://networknt.github.io/light-oauth2/tutorials/enterprise/)

Register a client that calls api_a.

```
curl -H "Content-Type: application/json" -X POST -d '{"clientType":"public","clientProfile":"mobile","clientName":"Consumer","clientDesc":"A client that calls API A","scope":"api_a.r api_a.w","redirectUri": "http://localhost:8080/authorization","ownerId":"admin"}' http://localhost:6884/oauth2/client

```

The return value is
```
{"clientId":"f0439841-fbe7-43a4-843e-ae0c51971a5e","clientSecret":"pu9aCVwmQjK2PET0_vOl9A","clientType":"public","clientProfile":"mobile","clientName":"Consumer","clientDesc":"A client that calls API A","ownerId":"admin","scope":"api_a.r api_a.w","redirectUri":"http://localhost:8080/authorization","createDt":"2017-03-30","updateDt":null}
```

We will need to use this clientId and clientSecret to generate an access token later on.

Register a client for api_a to call api_b

```
curl -H "Content-Type: application/json" -X POST -d '{"clientType":"public","clientProfile":"service","clientName":"api_a","clientDesc":"API A service","scope":"api_b.r api_b.w","redirectUri": "http://localhost:8080/authorization","ownerId":"admin"}' http://localhost:6884/oauth2/client

```

And the result is

```
{"clientId":"d5a0fa30-408b-4068-884c-e1f36c9e20e7","clientSecret":"DexYT2-OSHKQtNGsH0YYKQ","clientType":"public","clientProfile":"service","clientName":"api_a","clientDesc":"API A service","ownerId":"admin","scope":"api_b.r api_b.w","redirectUri":"http://localhost:8080/authorization","createDt":"2017-03-30","updateDt":null}
```

Now we need to externalize client.yml and update it with this clientId and clientSecret for
api_a. Let's create a config folder for api_a.

```
cd ~/networknt/light-java-example/ms_chain/config/api_a
mkdir security
cp apionly/api_a.yml security
```

Let's create client.yml file in the newly created security folder.

```
description: client configuration, all timing is milli-second
sync:
  maxConnectionTotal: 100
  maxConnectionPerRoute: 10
  routes:
    api.google.com: 20
    api.facebook.com: 10
  timeout: 10000
  keepAlive: 15000
async:
  maxConnectionTotal: 100
  maxConnectionPerRoute: 10
  routes:
    api.google.com: 20
    api.facebook.com: 10
  reactor:
    ioThreadCount: 1
    connectTimeout: 10000
    soTimeout: 10000
  timeout: 10000
  keepAlive: 15000
tls:
  verifyHostname: false
  loadTrustStore: false
  trustStore: trust.keystore
  trustPass: password
  loadKeyStore: false
  keyStore: key.jks
  keyPass: password
oauth:
  tokenRenewBeforeExpired: 600000
  expiredRefreshRetryDelay: 5000
  earlyRefreshRetryDelay: 30000
  server_url: http://oauth2-token:6882
  authorization_code:
    uri: "/oauth2/token"
    client_id: d5a0fa30-408b-4068-884c-e1f36c9e20e7
    client_secret: DexYT2-OSHKQtNGsH0YYKQ
    redirect_uri: https://localhost:8080/authorization_code
    scope:
    - api_b.r
    - api_b.w
  client_credentials:
    uri: "/oauth2/token"
    client_id: d5a0fa30-408b-4068-884c-e1f36c9e20e7
    client_secret: DexYT2-OSHKQtNGsH0YYKQ
    scope:
    - api_b.r
    - api_b.w

```

As you can see the server_url is pointing to OAuth2 token service at 6882. And
client_id and client_secret are updated according to the client register result.
Also, scope has been updated to api_b.r and api_b.w in roder to access API B.

Register a client for api_b to call api_c

```
curl -H "Content-Type: application/json" -X POST -d '{"clientType":"public","clientProfile":"service","clientName":"api_b","clientDesc":"API B service","scope":"api_c.r api_c.w","redirectUri": "http://localhost:8080/authorization","ownerId":"admin"}' http://localhost:6884/oauth2/client

```

And the result is

```
{"clientId":"2970c16d-d39b-4ccc-96b0-d6dc4325340f","clientSecret":"H4FpRXo_RLiNatcxce2d8g","clientType":"public","clientProfile":"service","clientName":"api_b","clientDesc":"API B service","ownerId":"admin","scope":"api_c.r api_c.w","redirectUri":"http://localhost:8080/authorization","createDt":"2017-03-30","updateDt":null}
```

Now we need to externalize client.yml and update it with this clientId and clientSecret for
api_b. Let's create a config folder for api_b.

```
cd ~/networknt/light-java-example/ms_chain/config/api_b
mkdir security
cp apionly/api_b.yml security
```

Let's create client.yml file in the newly created security folder.

```
description: client configuration, all timing is milli-second
sync:
  maxConnectionTotal: 100
  maxConnectionPerRoute: 10
  routes:
    api.google.com: 20
    api.facebook.com: 10
  timeout: 10000
  keepAlive: 15000
async:
  maxConnectionTotal: 100
  maxConnectionPerRoute: 10
  routes:
    api.google.com: 20
    api.facebook.com: 10
  reactor:
    ioThreadCount: 1
    connectTimeout: 10000
    soTimeout: 10000
  timeout: 10000
  keepAlive: 15000
tls:
  verifyHostname: false
  loadTrustStore: false
  trustStore: trust.keystore
  trustPass: password
  loadKeyStore: false
  keyStore: key.jks
  keyPass: password
oauth:
  tokenRenewBeforeExpired: 600000
  expiredRefreshRetryDelay: 5000
  earlyRefreshRetryDelay: 30000
  server_url: http://oauth2-token:6882
  authorization_code:
    uri: "/oauth2/token"
    client_id: 2970c16d-d39b-4ccc-96b0-d6dc4325340f
    client_secret: H4FpRXo_RLiNatcxce2d8g
    redirect_uri: https://localhost:8080/authorization_code
    scope:
    - api_c.r
    - api_c.w
  client_credentials:
    uri: "/oauth2/token"
    client_id: 2970c16d-d39b-4ccc-96b0-d6dc4325340f
    client_secret: H4FpRXo_RLiNatcxce2d8g
    scope:
    - api_c.r
    - api_c.w

```

Register a client for api_c to call api_d

```
curl -H "Content-Type: application/json" -X POST -d '{"clientType":"public","clientProfile":"service","clientName":"api_c","clientDesc":"API C service","scope":"api_d.r api_d.w","redirectUri": "http://localhost:8080/authorization","ownerId":"admin"}' http://localhost:6884/oauth2/client

```

And the result is

```
{"clientId":"50c92172-d223-4902-9779-df9ef501724f","clientSecret":"ZOz5tiF8TqmichIkVO9EPg","clientType":"public","clientProfile":"service","clientName":"api_c","clientDesc":"API C service","ownerId":"admin","scope":"api_d.r api_d.w","redirectUri":"http://localhost:8080/authorization","createDt":"2017-03-30","updateDt":null}
```

Now we need to externalize client.yml and update it with this clientId and clientSecret for
api_c. Let's create a config folder for api_c.

```
cd ~/networknt/light-java-example/ms_chain/config/api_c
mkdir security
cp apionly/api_c.yml security
```

Let's create client.yml file in the newly created security folder.

```
description: client configuration, all timing is milli-second
sync:
  maxConnectionTotal: 100
  maxConnectionPerRoute: 10
  routes:
    api.google.com: 20
    api.facebook.com: 10
  timeout: 10000
  keepAlive: 15000
async:
  maxConnectionTotal: 100
  maxConnectionPerRoute: 10
  routes:
    api.google.com: 20
    api.facebook.com: 10
  reactor:
    ioThreadCount: 1
    connectTimeout: 10000
    soTimeout: 10000
  timeout: 10000
  keepAlive: 15000
tls:
  verifyHostname: false
  loadTrustStore: false
  trustStore: trust.keystore
  trustPass: password
  loadKeyStore: false
  keyStore: key.jks
  keyPass: password
oauth:
  tokenRenewBeforeExpired: 600000
  expiredRefreshRetryDelay: 5000
  earlyRefreshRetryDelay: 30000
  server_url: http://oauth2-token:6882
  authorization_code:
    uri: "/oauth2/token"
    client_id: 50c92172-d223-4902-9779-df9ef501724f
    client_secret: ZOz5tiF8TqmichIkVO9EPg
    redirect_uri: https://localhost:8080/authorization_code
    scope:
    - api_d.r
    - api_d.w
  client_credentials:
    uri: "/oauth2/token"
    client_id: 50c92172-d223-4902-9779-df9ef501724f
    client_secret: ZOz5tiF8TqmichIkVO9EPg
    scope:
    - api_d.r
    - api_d.w

```

API D is not calling any other API so it is not a client and doesn't need to be registered.

Now we need to rebuild and restart API A, B, C and D. 

```
cd ~/networknt/light-java-example/ms_chain/api_a/security
mvn clean install
cd ~/networknt/light-java-example/ms_chain/api_b/security
mvn clean install
cd ~/networknt/light-java-example/ms_chain/api_c/security
mvn clean install
cd ~/networknt/light-java-example/ms_chain/api_d/security
mvn clean install

```

Now let's update docker-compose-app.yml in ms_chain/compose/security to point to the
config files from security folder under config.

```
#
# docker-compose-app.yml
#

version: '2'

#
# Services
#
services:


    #
    # Microservice: API A
    #
    apia:
        build: ~/networknt/light-java-example/ms_chain/api_a/security/
        ports:
            - "7001:7001"
        networks:
            - localnet
        volumes:
            - ~/networknt/light-java-example/ms_chain/config/api_a/security:/config

    #
    # Microservice: API B
    #
    apib:
        build: ~/networknt/light-java-example/ms_chain/api_b/security/
        ports:
            - "7002:7002"
        networks:
            - localnet
        volumes:
            - ~/networknt/light-java-example/ms_chain/config/api_b/security:/config

    #
    # Microservice: API C
    #
    apic:
        build: ~/networknt/light-java-example/ms_chain/api_c/security/
        ports:
            - "7003:7003"
        networks:
            - localnet
        volumes:
            - ~/networknt/light-java-example/ms_chain/config/api_c/security:/config

    #
    # Microservice: API D
    #
    apid:
        build: ~/networknt/light-java-example/ms_chain/api_d/security/
        ports:
            - "7004:7004"
        networks:
            - localnet
        volumes:
            - ~/networknt/light-java-example/ms_chain/config/api_d/security:/config

#
# Networks
#
networks:
    localnet:
        external: true
```

Now let's start these services.

```
cd ~/networknt/light-java-example/ms_chain/compose/security
docker-compose -f docker-compose-app.yml up
```

Now we have both APIs and OAuth2 services running.

Let's create a token that can access API A. Remember we have created a consumer client?
we need that client id and client secret to create an access token.

```
curl -H "Authorization: Basic f0439841-fbe7-43a4-843e-ae0c51971a5e:pu9aCVwmQjK2PET0_vOl9A" -H "Content-Type: application/x-www-form-urlencoded" -X POST -d "grant_type=client_credentials" http://localhost:6882/oauth2/token
```

And here is the response

```
{"access_token":"eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTQ5MDgzNzQxNiwianRpIjoiam1vNTFuYTZ1VkUzX3YwaTNxeE5GUSIsImlhdCI6MTQ5MDgzNjgxNiwibmJmIjoxNDkwODM2Njk2LCJ2ZXJzaW9uIjoiMS4wIiwiY2xpZW50X2lkIjoiZjA0Mzk4NDEtZmJlNy00M2E0LTg0M2UtYWUwYzUxOTcxYTVlIiwic2NvcGUiOlsiYXBpX2EuciIsImFwaV9hLnciXX0.mmh-CDll2wH__STv2QgPk9v7p8f5TEBh8XeZyV6q6LUpuQEAhetjVGampz91b1ncn8kmuZQ-WP0q6UAgXq2CavNz3wDa1uvPFmOd0LY7p-Q7vlMdSj3UG6y-4CaP2Keqj7znq0YJUwGNzerQd9HkC6NmPrdUYCXWiIWNENDYqua9xT3d4Sc1lbVWczPsjCovNrXcCo8HTFBO_d5sPi5-0pFcLJ-KszHCzWaSMt7lGfvJX5psVzFf8vO5yurjfriGyBJ4Cdq6aWwMsxoN7PXJB8izFMlDq8UuW6IXNvYRct2sIknlP__UKdqKEP5R7v8dMKlWitcyxqFD-hWRCSbU0w","token_type":"bearer","expires_in":600}
```

Run the following command to call api_a with the token generated above.

```
curl -H "Authorization: Bearer eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTQ5MDgzNzQxNiwianRpIjoiam1vNTFuYTZ1VkUzX3YwaTNxeE5GUSIsImlhdCI6MTQ5MDgzNjgxNiwibmJmIjoxNDkwODM2Njk2LCJ2ZXJzaW9uIjoiMS4wIiwiY2xpZW50X2lkIjoiZjA0Mzk4NDEtZmJlNy00M2E0LTg0M2UtYWUwYzUxOTcxYTVlIiwic2NvcGUiOlsiYXBpX2EuciIsImFwaV9hLnciXX0.mmh-CDll2wH__STv2QgPk9v7p8f5TEBh8XeZyV6q6LUpuQEAhetjVGampz91b1ncn8kmuZQ-WP0q6UAgXq2CavNz3wDa1uvPFmOd0LY7p-Q7vlMdSj3UG6y-4CaP2Keqj7znq0YJUwGNzerQd9HkC6NmPrdUYCXWiIWNENDYqua9xT3d4Sc1lbVWczPsjCovNrXcCo8HTFBO_d5sPi5-0pFcLJ-KszHCzWaSMt7lGfvJX5psVzFf8vO5yurjfriGyBJ4Cdq6aWwMsxoN7PXJB8izFMlDq8UuW6IXNvYRct2sIknlP__UKdqKEP5R7v8dMKlWitcyxqFD-hWRCSbU0w" localhost:7001/v1/data
```

And here is the result.

```
["API C: Message 1","API C: Message 2","API D: Message 1","API D: Message 2","API B: Message 1","API B: Message 2","API A: Message 1","API A: Message 2"]
```

At this moment, all four APIs are protected by JWT token and API B, C, D are projected by scope additionally. 

We have went though the process to register clients and update client.yml with clientId
and clientSecret for each services except API D. It is a good learning experience but the
process is very slow and error prone. You can follow this on your own to learn interactions
with OAuth2 services but for people who don't want to do it manually, I will put the these
client registraion info into our database script so that it should work once the OAuth2
services are up and running. 

Above we've recorded all the output for each service registrations and I am going to add
insert statements into light-docker/light-oauth2/mysql/create_mysql.sql

Our OAuth2 servers support Oracle and Postgres as well and we are going to change these
scripts in their corresponding folders.

The other benefit to get these clients into the startup scirpt is to avoid redo it every
time the server is restarted. 

Here is the insert statements.

```

```


## Enable Metrics

When you start a service, you might realized that every few minutes, "InfluxDbReporter 
report is called" will be shown up on the console. By default, all services will try
to report metrics info to Influxdb and subsequently viewed from Grafana dashboard.

If InfluxDB is not available this report will be a noop.

In order to output to the right Influxdb instance, we need to externalize metrics.yml

Let's create a new folder metrics under compose and config.

```
cd ~/networknt/light-java-example/ms_chain/config/api_a
mkdir metrics
cp security/* metrics
cd ~/networknt/light-java-example/ms_chain/config/api_b
mkdir metrics
cp security/* metrics
cd ~/networknt/light-java-example/ms_chain/config/api_c
mkdir metrics
cp security/* metrics
cd ~/networknt/light-java-example/ms_chain/config/api_d
mkdir metrics

```

Now we need to add the following metrics.yml to each metrics folder under config.

```
description: Metrics handler configuration
enabled: true
influxdbProtocol: http
influxdbHost: influxdb
influxdbPort: 8086
influxdbName: metrics
influxdbUser: admin
influxdbPass: admin
reportInMinutes: 1

```


Now let's start Influxdb and Grafana from docker-compose-metrics.yml in light-docker.
The light-docker repo should have been checked out at preparation step.

```
cd ~/networknt/light-docker
docker-compose -f docker-compose-metrics.yml up
```

Let's update ms_chain/compose to create a new compose under metrics

```
cd ~/networknt/light-java-example/ms_chain/compose
mkdir metrics

```
Now create a new compose file under metrics folder

```
#
# docker-compose-app.yml
#

version: '2'

#
# Services
#
services:


    #
    # Microservice: API A
    #
    apia:
        build: ~/networknt/light-java-example/ms_chain/api_a/security/
        ports:
            - "7001:7001"
        networks:
            - localnet
        volumes:
            - ~/networknt/light-java-example/ms_chain/config/api_a/metrics:/config

    #
    # Microservice: API B
    #
    apib:
        build: ~/networknt/light-java-example/ms_chain/api_b/security/
        ports:
            - "7002:7002"
        networks:
            - localnet
        volumes:
            - ~/networknt/light-java-example/ms_chain/config/api_b/metrics:/config

    #
    # Microservice: API C
    #
    apic:
        build: ~/networknt/light-java-example/ms_chain/api_c/security/
        ports:
            - "7003:7003"
        networks:
            - localnet
        volumes:
            - ~/networknt/light-java-example/ms_chain/config/api_c/metrics:/config

    #
    # Microservice: API D
    #
    apid:
        build: ~/networknt/light-java-example/ms_chain/api_d/security/
        ports:
            - "7004:7004"
        networks:
            - localnet
        volumes:
            - ~/networknt/light-java-example/ms_chain/config/api_d/metrics:/config

#
# Networks
#
networks:
    localnet:
        external: true

```

Now shutdown the APP compose by CTRL+C and restart it.

```
cd ~/networknt/light-java-example/ms_chain/compose/metrics
docker-compose -f docker-compose-app.yml up
```

Let's use curl to access API A, this time I am using a long lived token I generated 
from a utility.

```
eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTgwNjIwMDY2MSwianRpIjoibmQtb2ZZbWRIY0JZTUlEYU50MUFudyIsImlhdCI6MTQ5MDg0MDY2MSwibmJmIjoxNDkwODQwNTQxLCJ2ZXJzaW9uIjoiMS4wIiwidXNlcl9pZCI6IlN0ZXZlIiwidXNlcl90eXBlIjoiRU1QTE9ZRUUiLCJjbGllbnRfaWQiOiJmN2Q0MjM0OC1jNjQ3LTRlZmItYTUyZC00YzU3ODc0MjFlNzIiLCJzY29wZSI6WyJhcGlfYS53IiwiYXBpX2IudyIsImFwaV9jLnciLCJhcGlfZC53Iiwic2VydmVyLmluZm8uciJdfQ.SPHICXRY4SuUvWf0NYtwUrQ2-N-NeYT3b4CvxbzNl7D7GL5CF91G3siECrRBVexe0smBHHeiP3bq65rnCVFtwlYYqH6ZS5P7-AFiNcLBzSI9-OhV8JSf5sv381nk2f41IE4av2YUlgY0_mcIDo24ItnuPCxj0l49CAaLb7b1SHZJBQJANJTeQj-wgFsEqwafA-2wH2gehtH8CmOuuYfWO5t5IehP-zJNVT66E4UTRfvvZaJIvNTEQBWPpaZeeK6e56SyBqaLOR7duqJZ8a2UQZRWsDdIVt2Y5jGXQu1gyenIvCQbYLS6iglg6Xaco9emnYFopd2i3psathuX367fvw

```



```
curl -H "Authorization: Bearer eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTgwNjIwMDY2MSwianRpIjoibmQtb2ZZbWRIY0JZTUlEYU50MUFudyIsImlhdCI6MTQ5MDg0MDY2MSwibmJmIjoxNDkwODQwNTQxLCJ2ZXJzaW9uIjoiMS4wIiwidXNlcl9pZCI6IlN0ZXZlIiwidXNlcl90eXBlIjoiRU1QTE9ZRUUiLCJjbGllbnRfaWQiOiJmN2Q0MjM0OC1jNjQ3LTRlZmItYTUyZC00YzU3ODc0MjFlNzIiLCJzY29wZSI6WyJhcGlfYS53IiwiYXBpX2IudyIsImFwaV9jLnciLCJhcGlfZC53Iiwic2VydmVyLmluZm8uciJdfQ.SPHICXRY4SuUvWf0NYtwUrQ2-N-NeYT3b4CvxbzNl7D7GL5CF91G3siECrRBVexe0smBHHeiP3bq65rnCVFtwlYYqH6ZS5P7-AFiNcLBzSI9-OhV8JSf5sv381nk2f41IE4av2YUlgY0_mcIDo24ItnuPCxj0l49CAaLb7b1SHZJBQJANJTeQj-wgFsEqwafA-2wH2gehtH8CmOuuYfWO5t5IehP-zJNVT66E4UTRfvvZaJIvNTEQBWPpaZeeK6e56SyBqaLOR7duqJZ8a2UQZRWsDdIVt2Y5jGXQu1gyenIvCQbYLS6iglg6Xaco9emnYFopd2i3psathuX367fvw" localhost:7001/v1/data
```


## Enable Logging



## Integration

## Performance

## Production

## Conclusion

