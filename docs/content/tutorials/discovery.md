---
date: 2017-01-27T20:57:14-05:00
title: Registry and Discovery
---

# Introduction

This is a tutorial to show you how to use service registry and discovery
for microservices. We are going to use api_a, api_b, api_c and api_d as
our examples. To simply the tutorial, I am going to disable the security
all the time.

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

We are going to generate the code into light-example-4j repo, so let's
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
using Intellij IDEA Community Edition, you need to open light-example-4j
repo and then import each project by right click pom.xml in each static folder.

As indicated from the title, here we are going to hard code urls in API to API
calls in configuration files. That means these services will be deployed on the 
known hosts with known ports. And we will have a config file for each project to 
define the calling service urls.

### API A

For API A, as it is calling API B and API C, its handler needs to be changed to
calling two other APIs and needs to load a configuration file that define the 
urls for API B and API C.

DataGetHandler.java

```
package com.networknt.apia.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.client.Client;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;

public class DataGetHandler implements HttpHandler {
    static String CONFIG_NAME = "api_a";
    static String apibUrl = (String)Config.getInstance().getJsonMapConfig(CONFIG_NAME).get("api_b_endpoint");
    static String apicUrl = (String) Config.getInstance().getJsonMapConfig(CONFIG_NAME).get("api_c_endpoint");

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        List<String> list = new Vector<String>();
        final HttpGet[] requests = new HttpGet[] {
                new HttpGet(apibUrl),
                new HttpGet(apicUrl),
        };
        try {
            CloseableHttpAsyncClient client = Client.getInstance().getAsyncClient();
            final CountDownLatch latch = new CountDownLatch(requests.length);
            for (final HttpGet request: requests) {
                //Client.getInstance().propagateHeaders(request, exchange);
                client.execute(request, new FutureCallback<HttpResponse>() {
                    @Override
                    public void completed(final HttpResponse response) {
                        try {
                            List<String> apiList = (List<String>) Config.getInstance().getMapper().readValue(response.getEntity().getContent(),
                                    new TypeReference<List<String>>(){});
                            list.addAll(apiList);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        latch.countDown();
                    }

                    @Override
                    public void failed(final Exception ex) {
                        ex.printStackTrace();
                        latch.countDown();
                    }

                    @Override
                    public void cancelled() {
                        System.out.println("cancelled");
                        latch.countDown();
                    }
                });
            }
            latch.await();
        } catch (ClientException e) {
            e.printStackTrace();
            throw new Exception("ClientException:", e);
        }
        // now add API A specific messages
        list.add("API A: Message 1");
        list.add("API A: Message 2");
        exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(list));
    }
}

```

The following is the config file that define the url for API B and API C. This is hard
coded and can only be changed in this config file and restart the server. For now, I
am just changing the file in src/main/resources/config folder, but it should be externalized
on official environment.

api_a.json

```
{
  "description": "api_a config",
  "api_b_endpoint": "http://localhost:7002/v1/data",
  "api_c_endpoint": "http://localhost:7003/v1/data"
}

```

As default port for generated server is 8080, we need to change API A to 7001 so that
we can start all servers on the same host.

server.json

```
{
  "description": "server config",
  "ip": "0.0.0.0",
  "port": 7001,
  "serviceId": "com.networknt.apia-1.0.0"
}

```


### API B

Change the handler to call API D and load configuration for API D url.

DataGetHandler.java

```
package com.networknt.apib.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.client.Client;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

public class DataGetHandler implements HttpHandler {
    static String CONFIG_NAME = "api_b";
    static String apidUrl = (String) Config.getInstance().getJsonMapConfig(CONFIG_NAME).get("api_d_endpoint");


    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        List<String> list = new ArrayList<String>();
        try {
            CloseableHttpClient client = Client.getInstance().getSyncClient();
            HttpGet httpGet = new HttpGet(apidUrl);
            //Client.getInstance().propagateHeaders(httpGet, exchange);
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
}

```

Configuration file for API D url.

api_b.json

```
{
  "description": "api_b config",
  "api_d_endpoint": "http://localhost:7004/v1/data"
}

```

Change port number for API B to 7002 from 8080.
server.json

```
{
  "description": "server config",
  "ip": "0.0.0.0",
  "port": 7002,
  "serviceId": "com.networknt.apib-1.0.0"
}

```

### API C


Update API C handler to return information that associates with API C.

DataGetHandler.java

```
package com.networknt.apic.handler;

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
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        List<String> messages = new ArrayList<String>();
        messages.add("API C: Message 1");
        messages.add("API C: Message 2");
        exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(messages));
    }
}

```

Update port number for API C to 7003 from 8080.

server.json

```
{
  "description": "server config",
  "ip": "0.0.0.0",
  "port": 7003,
  "serviceId": "com.networknt.apic-1.0.0"
}

```


### API D

Update Handler for API D to return messages related to API D.

DataGetHandler.java

```
package com.networknt.apid.handler;

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
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        List<String> messages = new ArrayList<String>();
        messages.add("API D: Message 1");
        messages.add("API D: Message 2");
        exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(messages));
    }
}

```


Update port to 7004 from 8080

server.json

```
{
  "description": "server config",
  "ip": "0.0.0.0",
  "port": 7004,
  "serviceId": "com.networknt.apid-1.0.0"
}

```

### Start Servers

Now let's start all four servers from four terminals.

API A

```
cd ~/networknt/light-java-example/discovery/api_a/static
mvn clean install exec:exec
```

API B

```
cd ~/networknt/light-java-example/discovery/api_b/static
mvn clean install exec:exec

```

API C

```
cd ~/networknt/light-java-example/discovery/api_c/static
mvn clean install exec:exec

```

API D

```
cd ~/networknt/light-java-example/discovery/api_d/static
mvn clean install exec:exec

```

### Test Servers

Let's access API A and see if we can get messages from all four servers.

```
curl http://localhost:7001/v1/data

```
The result is 

```
["API C: Message 1","API C: Message 2","API D: Message 1","API D: Message 2","API B: Message 1","API B: Message 2","API A: Message 1","API A: Message 2"]
```


# Dynamic

The above step uses static urls defined in configuration files. It won't work in a
dynamic clustered environment as there are more instances of each service. In this
step, we are going to use cluster component with direct registry so that we don't
need to start external consul or zookeeper instances. We still go through registry
for service discovery but the registry is defined in service.json. Next step we
will use consul server for the discovery to mimic real production environment.


First let's create a folder from static to dynamic.

```
cd ~/networknt/light-java-example/discovery/api_a
cp -r static dynamic
cd ~/networknt/light-java-example/discovery/api_b
cp -r static dynamic
cd ~/networknt/light-java-example/discovery/api_c
cp -r static dynamic
cd ~/networknt/light-java-example/discovery/api_d
cp -r static dynamic
```


### API A

Let's update API A Handler to use Cluster instance instead of using static config
files. 

DataGetHandler.java

```
package com.networknt.apia.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.client.Client;
import com.networknt.cluster.Cluster;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import com.networknt.service.SingletonServiceFactory;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import java.io.IOException;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataGetHandler implements HttpHandler {
    private static Logger logger = LoggerFactory.getLogger(DataGetHandler.class);
    private static Cluster cluster = (Cluster) SingletonServiceFactory.getBean(Cluster.class);

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        List<String> list = new Vector<>();

        String apibUrl = cluster.serviceToUrl("http", "com.networknt.apib-1.0.0") + "/v1/data";
        if(logger.isDebugEnabled()) logger.debug("apibUrl = " + apibUrl);
        String apicUrl = cluster.serviceToUrl("http", "com.networknt.apic-1.0.0") + "/v1/data";
        if(logger.isDebugEnabled()) logger.debug("apicUrl = " + apicUrl);
        final HttpGet[] requests = new HttpGet[] {
                new HttpGet(apibUrl),
                new HttpGet(apicUrl),
        };
        try {
            CloseableHttpAsyncClient client = Client.getInstance().getAsyncClient();
            final CountDownLatch latch = new CountDownLatch(requests.length);
            for (final HttpGet request: requests) {
                //Client.getInstance().propagateHeaders(request, exchange);
                client.execute(request, new FutureCallback<HttpResponse>() {
                    @Override
                    public void completed(final HttpResponse response) {
                        try {
                            List<String> apiList = Config.getInstance().getMapper().readValue(response.getEntity().getContent(),
                                    new TypeReference<List<String>>(){});
                            list.addAll(apiList);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        latch.countDown();
                    }

                    @Override
                    public void failed(final Exception ex) {
                        ex.printStackTrace();
                        latch.countDown();
                    }

                    @Override
                    public void cancelled() {
                        System.out.println("cancelled");
                        latch.countDown();
                    }
                });
            }
            latch.await();
        } catch (ClientException e) {
            e.printStackTrace();
            throw new Exception("ClientException:", e);
        }
        // now add API A specific messages
        list.add("API A: Message 1");
        list.add("API A: Message 2");
        exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(list));
    }
}

```

For discovery, some new modules should be included into the pom.xml.

```
        <dependency>
            <groupId>com.networknt</groupId>
            <artifactId>service</artifactId>
            <version>${version.light-java}</version>
        </dependency>
        <dependency>
            <groupId>com.networknt</groupId>
            <artifactId>registry</artifactId>
            <version>${version.light-java}</version>
        </dependency>
        <dependency>
            <groupId>com.networknt</groupId>
            <artifactId>balance</artifactId>
            <version>${version.light-java}</version>
        </dependency>
        <dependency>
            <groupId>com.networknt</groupId>
            <artifactId>cluster</artifactId>
            <version>${version.light-java}</version>
        </dependency>
        <dependency>
            <groupId>com.networknt</groupId>
            <artifactId>consul</artifactId>
            <version>${version.light-java}</version>
        </dependency>
```

Also, we need service.json to inject several singleton implementations of
Cluster, LoadBanlance, URL and Registry. Please note that the key in parameters
is serviceId of your calling APIs


```
{
  "description": "singleton service factory configuration",
  "singletons": [
    {
      "com.networknt.registry.URL": [
        {
          "com.networknt.registry.URLImpl": {
            "protocol": "https",
            "host": "localhost",
            "port": 8080,
            "path": "direct",
            "parameters": {
              "com.networknt.apib-1.0.0": "http://localhost:7002",
              "com.networknt.apic-1.0.0": "http://localhost:7003"
            }
          }
        }
      ]
    },
    {
      "com.networknt.registry.Registry" : [
        "com.networknt.registry.support.DirectRegistry"
      ]
    },
    {
      "com.networknt.balance.LoadBalance" : [
        "com.networknt.balance.RoundRobinLoadBalance"
      ]
    },
    {
      "com.networknt.cluster.Cluster" : [
        "com.networknt.cluster.LightCluster"
      ]
    }
  ]
}

```


### API B

DataGetHandler.java

```
package com.networknt.apib.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.client.Client;
import com.networknt.cluster.Cluster;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import com.networknt.service.SingletonServiceFactory;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataGetHandler implements HttpHandler {
    private static Logger logger = LoggerFactory.getLogger(DataGetHandler.class);
    private static Cluster cluster = (Cluster) SingletonServiceFactory.getBean(Cluster.class);

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        List<String> list = new ArrayList<>();
        String apidUrl = cluster.serviceToUrl("http", "com.networknt.apid-1.0.0") + "/v1/data";
        if(logger.isDebugEnabled()) logger.debug("apidUrl = " + apidUrl);

        try {
            CloseableHttpClient client = Client.getInstance().getSyncClient();
            HttpGet httpGet = new HttpGet(apidUrl);
            //Client.getInstance().propagateHeaders(httpGet, exchange);
            CloseableHttpResponse response = client.execute(httpGet);
            int responseCode = response.getStatusLine().getStatusCode();
            if(responseCode != 200){
                throw new Exception("Failed to call API D: " + responseCode);
            }
            List<String> apidList = Config.getInstance().getMapper().readValue(response.getEntity().getContent(),
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
}

```

As API B is calling API D, it needs discovery as well and the following dependencies
should be added to pom.xml

```
        <dependency>
            <groupId>com.networknt</groupId>
            <artifactId>service</artifactId>
            <version>${version.light-java}</version>
        </dependency>
        <dependency>
            <groupId>com.networknt</groupId>
            <artifactId>registry</artifactId>
            <version>${version.light-java}</version>
        </dependency>
        <dependency>
            <groupId>com.networknt</groupId>
            <artifactId>balance</artifactId>
            <version>${version.light-java}</version>
        </dependency>
        <dependency>
            <groupId>com.networknt</groupId>
            <artifactId>cluster</artifactId>
            <version>${version.light-java}</version>
        </dependency>
        <dependency>
            <groupId>com.networknt</groupId>
            <artifactId>consul</artifactId>
            <version>${version.light-java}</version>
        </dependency>

```

Inject interface implementations and define the API D url.

```
{
  "description": "singleton service factory configuration",
  "singletons": [
    {
      "com.networknt.registry.URL": [
        {
          "com.networknt.registry.URLImpl": {
            "protocol": "https",
            "host": "localhost",
            "port": 8080,
            "path": "direct",
            "parameters": {
              "com.networknt.apid-1.0.0": "http://localhost:7004"
            }
          }
        }
      ]
    },
    {
      "com.networknt.registry.Registry" : [
        "com.networknt.registry.support.DirectRegistry"
      ]
    },
    {
      "com.networknt.balance.LoadBalance" : [
        "com.networknt.balance.RoundRobinLoadBalance"
      ]
    },
    {
      "com.networknt.cluster.Cluster" : [
        "com.networknt.cluster.LightCluster"
      ]
    }
  ]
}

```


### API C

API C is not calling any other APIs, so there is no change to its handler.




### API D

API D is not calling any other APIs, so there is no change to its handler.


### Start Servers

Now let's start all four servers from four terminals.

API A

```
cd ~/networknt/light-java-example/discovery/api_a/dynamic
mvn clean install exec:exec
```

API B

```
cd ~/networknt/light-java-example/discovery/api_b/dynamic
mvn clean install exec:exec

```

API C

```
cd ~/networknt/light-java-example/discovery/api_c/dynamic
mvn clean install exec:exec

```

API D

```
cd ~/networknt/light-java-example/discovery/api_d/dynamic
mvn clean install exec:exec

```

### Test Servers

Let's access API A and see if we can get messages from all four servers.

```
curl http://localhost:7001/v1/data

```
The result is 

```
["API C: Message 1","API C: Message 2","API D: Message 1","API D: Message 2","API B: Message 1","API B: Message 2","API A: Message 1","API A: Message 2"]
```

# Multiple API D Instances

In this step, we are going to start two API D instances that listening to 70041 and 70042.

Now let's copy from dynamic to multiple for each API.
 

```
cd ~/networknt/light-java-example/discovery/api_a
cp -r dynamic multiple
cd ~/networknt/light-java-example/discovery/api_b
cp -r dynamic multiple
cd ~/networknt/light-java-example/discovery/api_c
cp -r dynamic multiple
cd ~/networknt/light-java-example/discovery/api_d
cp -r dynamic multiple
```

 
### API B 

Let's modify API B service.json to have two API D instances that listen to 70041
and 70042. 

service.json

```
{
  "description": "singleton service factory configuration",
  "singletons": [
    {
      "com.networknt.registry.URL": [
        {
          "com.networknt.registry.URLImpl": {
            "protocol": "https",
            "host": "localhost",
            "port": 8080,
            "path": "direct",
            "parameters": {
              "com.networknt.apid-1.0.0": "http://localhost:7004,http://localhost:7005"
            }
          }
        }
      ]
    },
    {
      "com.networknt.registry.Registry" : [
        "com.networknt.registry.support.DirectRegistry"
      ]
    },
    {
      "com.networknt.balance.LoadBalance" : [
        "com.networknt.balance.RoundRobinLoadBalance"
      ]
    },
    {
      "com.networknt.cluster.Cluster" : [
        "com.networknt.cluster.LightCluster"
      ]
    }
  ]
}

```

### API D

In order to start two instances with the same code base, we need to modify the
server.json before starting the server. 

Also, let's update the handler so that we know which port serves the request.

DataGetHandler.java

```
package com.networknt.apid.handler;

import com.networknt.config.Config;
import com.networknt.server.Server;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringEscapeUtils;

public class DataGetHandler implements HttpHandler {
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        int port = Server.config.getPort();
        List<String> messages = new ArrayList<String>();
        messages.add("API D: Message 1 from port " + port);
        messages.add("API D: Message 2 from port " + port);
        exchange.getResponseSender().send(Config.getInstance().getMapper().writeValueAsString(messages));
    }
}

```

### Start Servers
Now let's start all five servers from five terminals. API D has two instances.

API A

```
cd ~/networknt/light-java-example/discovery/api_a/multiple
mvn clean install exec:exec
```

API B

```
cd ~/networknt/light-java-example/discovery/api_b/multiple
mvn clean install exec:exec

```

API C

```
cd ~/networknt/light-java-example/discovery/api_c/multiple
mvn clean install exec:exec

```

API D


And start the first instance that listen to 7004.

```
cd ~/networknt/light-java-example/discovery/api_d/multiple
mvn clean install exec:exec

```
 
Now let's start the second instance. Before starting the serer, let's update
server.json with port 7005.

```
{
  "description": "server config",
  "ip": "0.0.0.0",
  "port": 7005,
  "serviceId": "com.networknt.apid-1.0.0"
}

```

And start the second instance

```
cd ~/networknt/light-java-example/discovery/api_d/multiple
mvn clean install exec:exec

```

### Test Servers

```
curl http://localhost:7001/v1/data
```

And the result can be the following alternatively.

```
["API C: Message 1","API C: Message 2","API D: Message 1 from port 7004","API D: Message 2 from port 7004","API B: Message 1","API B: Message 2","API A: Message 1","API A: Message 2"]
```

or 

```
["API C: Message 1","API C: Message 2","API D: Message 1 from port 7005","API D: Message 2 from port 7005","API B: Message 1","API B: Message 2","API A: Message 1","API A: Message 2"]
```

# Consul

Above step multiple demonstrates how to use direct registry to enable load balance and
it works the same way as Consul and Zookeeper registry. In this step, we are going to
use Consul for registry to enable cluster.

Now let's copy from multiple to consul for each API.
 

```
cd ~/networknt/light-java-example/discovery/api_a
cp -r multiple consul
cd ~/networknt/light-java-example/discovery/api_b
cp -r multiple consul
cd ~/networknt/light-java-example/discovery/api_c
cp -r multiple consul
cd ~/networknt/light-java-example/discovery/api_d
cp -r multiple consul
```


### API A

In order to switch from direct registry to consul registry, we just need to update
service.json configuration to inject the consul implementation to the registry interface.

service.json

```
{
  "description": "singleton service factory configuration",
  "singletons": [
    {
      "com.networknt.registry.URL": [
        {
          "com.networknt.registry.URLImpl": {
            "protocol": "light",
            "host": "localhost",
            "port": 8080,
            "path": "consul",
            "parameters": {
              "registryRetryPeriod": "30000"
            }
          }
        }
      ]
    },
    {
      "com.networknt.consul.client.ConsulClient": [
        {
          "com.networknt.consul.client.ConsulEcwidClient": [
            {"java.lang.String": "localhost"},
            {"int": 8500}
          ]
        }
      ]
    },
    {
      "com.networknt.registry.Registry" : [
        "com.networknt.consul.ConsulRegistry"
      ]
    },
    {
      "com.networknt.balance.LoadBalance" : [
        "com.networknt.balance.RoundRobinLoadBalance"
      ]
    },
    {
      "com.networknt.cluster.Cluster" : [
        "com.networknt.cluster.LightCluster"
      ]
    }
  ]
}
```

Although in our case, there is no caller service for API A, we still need to register
it to consul by enable it in server.json

```
{
  "description": "server config",
  "ip": "0.0.0.0",
  "port": 7001,
  "serviceId": "com.networknt.apia-1.0.0",
  "enableRegistry": true
}

```


### API B

Let's update service.json to inject consul registry instead of direct registry used in
the previous step.

service.json

```
{
  "description": "singleton service factory configuration",
  "singletons": [
    {
      "com.networknt.registry.URL": [
        {
          "com.networknt.registry.URLImpl": {
            "protocol": "light",
            "host": "localhost",
            "port": 8080,
            "path": "consul",
            "parameters": {
              "registryRetryPeriod": "30000"
            }
          }
        }
      ]
    },
    {
      "com.networknt.consul.client.ConsulClient": [
        {
          "com.networknt.consul.client.ConsulEcwidClient": [
            {"java.lang.String": "localhost"},
            {"int": 8500}
          ]
        }
      ]
    },
    {
      "com.networknt.registry.Registry" : [
        "com.networknt.consul.ConsulRegistry"
      ]
    },
    {
      "com.networknt.balance.LoadBalance" : [
        "com.networknt.balance.RoundRobinLoadBalance"
      ]
    },
    {
      "com.networknt.cluster.Cluster" : [
        "com.networknt.cluster.LightCluster"
      ]
    }
  ]
}
```

As API B will be called by API A, it needs to register itself to consul registry so
that API A can discover it through the same consul registry. To do that you only need
to enable server registry in config file.

server.json

```
{
  "description": "server config",
  "ip": "0.0.0.0",
  "port": 7002,
  "serviceId": "com.networknt.apib-1.0.0",
  "enableRegistry": true
}

```

### API C

Although API C is not calling any other APIs, it needs to register itself to consul
so that API A can discovery it from the same consul registry.

service.json

```
{
  "description": "singleton service factory configuration",
  "singletons": [
    {
      "com.networknt.registry.URL": [
        {
          "com.networknt.registry.URLImpl": {
            "protocol": "light",
            "host": "localhost",
            "port": 8080,
            "path": "consul",
            "parameters": {
              "registryRetryPeriod": "30000"
            }
          }
        }
      ]
    },
    {
      "com.networknt.consul.client.ConsulClient": [
        {
          "com.networknt.consul.client.ConsulEcwidClient": [
            {"java.lang.String": "localhost"},
            {"int": 8500}
          ]
        }
      ]
    },
    {
      "com.networknt.registry.Registry" : [
        "com.networknt.consul.ConsulRegistry"
      ]
    },
    {
      "com.networknt.balance.LoadBalance" : [
        "com.networknt.balance.RoundRobinLoadBalance"
      ]
    },
    {
      "com.networknt.cluster.Cluster" : [
        "com.networknt.cluster.LightCluster"
      ]
    }
  ]
}
```


server.json

```
{
  "description": "server config",
  "ip": "0.0.0.0",
  "port": 7003,
  "serviceId": "com.networknt.apic-1.0.0",
  "enableRegistry": true
}

```

Also, in previous step, we didn't add extra dependencies for registry, load balance
cluster and consul modules. Let's add them here in pom.xml

```
        <dependency>
            <groupId>com.networknt</groupId>
            <artifactId>registry</artifactId>
            <version>${version.light-java}</version>
        </dependency>
        <dependency>
            <groupId>com.networknt</groupId>
            <artifactId>balance</artifactId>
            <version>${version.light-java}</version>
        </dependency>
        <dependency>
            <groupId>com.networknt</groupId>
            <artifactId>cluster</artifactId>
            <version>${version.light-java}</version>
        </dependency>
        <dependency>
            <groupId>com.networknt</groupId>
            <artifactId>consul</artifactId>
            <version>${version.light-java}</version>
        </dependency>

```


### API D

Although API D is not calling any other APIs, it needs to register itself to consul
so that API B can discovery it from the same consul registry.

service.json

```
{
  "description": "singleton service factory configuration",
  "singletons": [
    {
      "com.networknt.registry.URL": [
        {
          "com.networknt.registry.URLImpl": {
            "protocol": "light",
            "host": "localhost",
            "port": 8080,
            "path": "consul",
            "parameters": {
              "registryRetryPeriod": "30000"
            }
          }
        }
      ]
    },
    {
      "com.networknt.consul.client.ConsulClient": [
        {
          "com.networknt.consul.client.ConsulEcwidClient": [
            {"java.lang.String": "localhost"},
            {"int": 8500}
          ]
        }
      ]
    },
    {
      "com.networknt.registry.Registry" : [
        "com.networknt.consul.ConsulRegistry"
      ]
    },
    {
      "com.networknt.balance.LoadBalance" : [
        "com.networknt.balance.RoundRobinLoadBalance"
      ]
    },
    {
      "com.networknt.cluster.Cluster" : [
        "com.networknt.cluster.LightCluster"
      ]
    }
  ]
}
```

server.json

```
{
  "description": "server config",
  "ip": "0.0.0.0",
  "port": 7004,
  "serviceId": "com.networknt.apid-1.0.0",
  "enableRegistry": true
}

```

Also add extra dependencies to pom.xml to enable cluster.

```
        <dependency>
            <groupId>com.networknt</groupId>
            <artifactId>registry</artifactId>
            <version>${version.light-java}</version>
        </dependency>
        <dependency>
            <groupId>com.networknt</groupId>
            <artifactId>balance</artifactId>
            <version>${version.light-java}</version>
        </dependency>
        <dependency>
            <groupId>com.networknt</groupId>
            <artifactId>cluster</artifactId>
            <version>${version.light-java}</version>
        </dependency>
        <dependency>
            <groupId>com.networknt</groupId>
            <artifactId>consul</artifactId>
            <version>${version.light-java}</version>
        </dependency>

```


### Start Consul

Here we are starting consul server in docker to serve as a centralized registry. To make it
simpler, the ACL in consul is disable by setting acl_default_policy=allow.

```
docker run -d -p 8400:8400 -p 8500:8500/tcp -p 8600:53/udp -e 'CONSUL_LOCAL_CONFIG={"acl_datacenter":"dc1","acl_default_policy":"allow","acl_down_policy":"extend-cache","acl_master_token":"the_one_ring","bootstrap_expect":1,"datacenter":"dc1","data_dir":"/usr/local/bin/consul.d/data","server":true}' consul agent -server -ui -bind=127.0.0.1 -client=0.0.0.0
```


### Start four servers

Now let's start four terminals to start servers.  

API A

```
cd ~/networknt/light-java-example/discovery/api_a/consul
mvn clean install exec:exec
```

API B

```
cd ~/networknt/light-java-example/discovery/api_b/consul
mvn clean install exec:exec

```

API C

```
cd ~/networknt/light-java-example/discovery/api_c/consul
mvn clean install exec:exec

```

API D


And start the first instance that listen to 7004 as default

```
cd ~/networknt/light-java-example/discovery/api_d/consul
mvn clean install exec:exec

```

### Test four servers

```
curl http://localhost:7001/v1/data
```

And the result will be

```
["API C: Message 1","API C: Message 2","API D: Message 1 from port 7004","API D: Message 2 from port 7004","API B: Message 1","API B: Message 2","API A: Message 1","API A: Message 2"]
```
 
### Start another API D
 
Now let's start the second instance of API D. Before starting the serer, let's update
server.json with port 7005.

```
{
  "description": "server config",
  "ip": "0.0.0.0",
  "port": 7005,
  "serviceId": "com.networknt.apid-1.0.0"
}

```

And start the second instance

```
cd ~/networknt/light-java-example/discovery/api_d/consul
mvn clean install exec:exec

```

### Test Servers

Wait 10 seconds, your API B cached API D service urls will be updated automatically
with the new instance. Now you have to instance of API D to serve API B.

```
curl http://localhost:7001/v1/data
```

And the result can be the following alternatively.

```
["API C: Message 1","API C: Message 2","API D: Message 1 from port 7004","API D: Message 2 from port 7004","API B: Message 1","API B: Message 2","API A: Message 1","API A: Message 2"]
```

or 

```
["API C: Message 1","API C: Message 2","API D: Message 1 from port 7005","API D: Message 2 from port 7005","API B: Message 1","API B: Message 2","API A: Message 1","API A: Message 2"]
```


### Shutdown one API D

Let's shutdown one instance of API D and wait for 10 seconds. Now when you call the same
curl command, API D message will be always served by the same port which is the one still
running.


# Docker

In this step, we are going to dockerize all the APIs and then use registrator for service
registry. 

Now let's copy from consul to docker for each API.
 

```
cd ~/networknt/light-java-example/discovery/api_a
cp -r consul consuldocker
cd ~/networknt/light-java-example/discovery/api_b
cp -r consul consuldocker
cd ~/networknt/light-java-example/discovery/api_c
cp -r consul consuldocker
cd ~/networknt/light-java-example/discovery/api_d
cp -r consul consulcdocker
```

Before starting the services, let's start consul and registrator.

Consul

```
docker run -d -p 8400:8400 -p 8500:8500/tcp -p 8600:53/udp -e 'CONSUL_LOCAL_CONFIG={"acl_datacenter":"dc1","acl_default_policy":"allow","acl_down_policy":"extend-cache","acl_master_token":"the_one_ring","bootstrap_expect":1,"datacenter":"dc1","data_dir":"/usr/local/bin/consul.d/data","server":true}' consul agent -server -ui -bind=127.0.0.1 -client=0.0.0.0
```

Regsitrator

We use -ip 127.0.0.1 in the command line to make sure that ServiceAddress in
consul is populated with ip and port. The latest version of regsitrator won't
set default ip anymore.

```
docker run -d --name=registrator --net=host --volume=/var/run/docker.sock:/tmp/docker.sock gliderlabs/registrator:latest -ip 127.0.0.1 consul://localhost:8500
```


### API A

Since we are using registrator to register the service, we need to disable the application service registration.

server.json

```
{
  "description": "server config",
  "ip": "0.0.0.0",
  "port": 7001,
  "serviceId": "com.networknt.apia-1.0.0",
  "enableRegistry": false
}
```


```
cd ~/networknt/light-java-example/discovery/api_a/consuldocker
mvn clean install
docker build -t networknt/com.networknt.apia-1.0.0 .
docker run -it -p 7001:7001 --net=host --name=com.networknt.apia-1.0.0 networknt/com.networknt.apia-1.0.0
```

### API B

server.json

```
{
  "description": "server config",
  "ip": "0.0.0.0",
  "port": 7002,
  "serviceId": "com.networknt.apib-1.0.0",
  "enableRegistry": false
}

```

```
cd ~/networknt/light-java-example/discovery/api_b/consuldocker
mvn clean install
docker build -t networknt/com.networknt.apib-1.0.0 .
docker run -it -p 7002:7002 --net=host --name=com.networknt.apib-1.0.0 networknt/com.networknt.apib-1.0.0

```

### API C

server.json

```
{
  "description": "server config",
  "ip": "0.0.0.0",
  "port": 7003,
  "serviceId": "com.networknt.apic-1.0.0",
  "enableRegistry": false
}

```

```
cd ~/networknt/light-java-example/discovery/api_c/consuldocker
mvn clean install
docker build -t networknt/com.networknt.apic-1.0.0 .
docker run -it -p 7003:7003 --net=host --name=com.networknt.apic-1.0.0 networknt/com.networknt.apic-1.0.0

```

### API D

server.json

```
{
  "description": "server config",
  "ip": "0.0.0.0",
  "port": 7004,
  "serviceId": "com.networknt.apid-1.0.0",
  "enableRegistry": false
}

```

```
cd ~/networknt/light-java-example/discovery/api_d/consuldocker
mvn clean install
docker build -t networknt/com.networknt.apid-1.0.0 .
docker run -it -p 7004:7004 --net=host --name=com.networknt.apid-1.0.0 networknt/com.networknt.apid-1.0.0

```


### Test Servers

```
curl http://localhost:7001/v1/data
```

And here is the result.

```
["API C: Message 1","API C: Message 2","API D: Message 1 from port 7004","API D: Message 2 from port 7004","API B: Message 1","API B: Message 2","API A: Message 1","API A: Message 2"]
```

# Kubernetes

