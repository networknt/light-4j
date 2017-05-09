---
date: 2017-02-06T09:59:28-05:00
title: Correlation Id
---

This is a handler that checks if X-Correlation-Id exists in request header. If it doesn't exist
it will generate a UUID and put it into the request header. During API to API calls, this header
will be passed to the next API by [Client](https://networknt.github.io/light-4j/other/client/) 
module.

# Generating

The correlationId is very useful in microservices architecture as there are multiple services
involved in a same client request. When logs are aggregated into a centralized tool, it is
very important there is an unique identifier to associate logs from multiple services for the
same request. The Id is an UUID and must be generated in the first service called from client.

# Passing

Since the first service generates the Id, it must be passed to other services somehow so that
subsequent services can use it to log their messages. In our [client](https://networknt.github.io/light-4j/other/client/)
module, it passes the correlationId from the current request header to the request to the next
service.

# Logging

This handler gets the X-Correlation-Id from request header or generate one if it doesn't 
exist in the request header. After that, it puts it into the org.slf4j.MDC so that logback
can put it into the log for every logging statement. 

# logback.xml

In the generated logback.xml, the cId is part of the appender config as pattern "%X{cId}"

```
    <appender name="stdout" class="ch.qos.logback.core.ConsoleAppender">
        <!-- encoders are assigned the type
             ch.qos.logback.classic.encoder.PatternLayoutEncoder by default -->
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %X{cId} %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <appender name="log" class="ch.qos.logback.core.FileAppender">
        <File>target/test.log</File>
        <Append>false</Append>
        <layout class="ch.qos.logback.classic.PatternLayout">
            <Pattern>%d{HH:mm:ss.SSS} [%thread] %X{cId} %-5level %class{36}:%L %M - %msg%n</Pattern>
        </layout>
    </appender>

```
# Configuration

The configuration for this module is very simple. Just enable it or not. Here is the default
config in the module.

```
description: Correlation Handler
enabled: true

```
