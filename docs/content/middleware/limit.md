---
date: 2017-02-17T14:10:28-05:00
title: Rate Limiting
---

Although our framework can handle potential millions requests per second, for 
some public facing APIs, it might be a good idea to enable this handler to 
limit the concurrent request to certain level in order to avoid DDOS attacks.

As this handler will impact the overall performance a little bit, it is not
configured as default in the swagger-codegen. You must manually enable it
in your API with the following steps.

## Dependency

In order to use this handler, the following dependency need to be added to
pom.xml in your project.

```
            <dependency>
                <groupId>com.networknt</groupId>
                <artifactId>limit</artifactId>
                <version>${version.light-4j}</version>
            </dependency>
```

## Service

As this middleware is not plugged in by default, we need to add it into
com.networknt.handler.MiddlewareHandler in src/main/resources/META-INF/services
folder. As this rate limiting handler needs to be failed fast, it need to be
put right after ExceptionHandler and MetricsHandler. The reason it is after
MetricsHandler is to capture 513 error code in InfluxDB and Grafana for
monitoring on production.

```
#Traceability Put traceabilityId into response header from request header if it exists
com.networknt.traceability.TraceabilityHandler
#Rate Limiting
com.networknt.limit.LimitHandler
#Metrics In order to calculate response time accurately, this needs to be the second.
com.networknt.metrics.MetricsHandler
#Exception Global exception handler that needs to be called first.
com.networknt.exception.ExceptionHandler

```

## Config

Here is the configuration file for rate limiting.

```
{
  "description": "Rate Limit Handler",
  "enabled": false,
  "concurrentRequest": 1000,
  "queueSize": -1
}

```

- enabled true to enable it and false to disable it.
- concurrentRequest number of concurrent request to be limited.
- queueSize -1 unlimited queue size which might use a lot of memory. > 1 integer will limit the requests to be queued and once queue is full, 513 will be returned for new requests. 



