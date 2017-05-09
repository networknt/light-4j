---
date: 2016-10-12T18:48:58-04:00
title: Audit
---

There are two built-in audit handlers that write logs into audit.log that setup 
in the logback appender. End user can add more customized audit handlers if need.

In the audit module, there is AuditHandler which is a generic and configurable
with audit.yml config file. 

There is another audit provided by the light-4j framework called DumpHandler in
[dump](/middleware/dump/) module. 

## Introduction 

Only logs several fields from request header and the fields are configurable. 
Optional, it can log response status and response time.

This is a generic audit handler that dump most important info per request basis. 
The following elements will be logged if it's available in auditInfo object 
attached to exchange. This object wil be populated by other upstream handlers 
like swagger-meta and swagger-security for light-rest-4j framework.

This handler can be used on production but be aware that it will impact the 
overall performance. Turning off statusCode and responseTime can make it faster 
as these have to be captured on the response chain instead of request chain.

For most business and majority of microservices, you don't need to enable this 
handler due to performance reason. The default audit log will be audit.log config 
in the default logback.xml; however, it can be changed to syslog or Kafka with 
customized appender.

Majority of the fields in audit log are collected in request and response; 
however, to allow user to customize it, we have put an attachment into the 
exchange to allow other handlers to write important info into it. The audit.yml 
can control which fields should be included in the final log.

By default, the following fields are included:

 * timestamp
 * serviceId (from server.yml)
 * correlationId
 * traceabilityId (if available)
 * clientId
 * userId (if available)
 * scopeClientId (available if called by another API)
 * endpoint (uriPattern@method)
 * statusCode
 * responseTime

The audit.log is in JSON format and it is easy to be parsed and monitored. 

## Configuration
 
The output fields are populated based on the config file audit.yml and here is
and example. 

```
# AuditHandler will pick some important fields from headers and tokens and logs into a audit appender.
---
# Enable Audit
enabled: true

# Output response status code
statusCode: true

# Output response time
responseTime: true

# Output header elements. You can add more if you want.
headers:

# Correlation Id
- X-Correlation-Id

# Traceability Id
- X-Traceability-Id

# Output from id token and access token
audit:

# Service Id
- service_id

# Client Id
- client_id

# User Id
- user_id

# Client Id in scope/access token
- scope_client_id

# Request endpoint uri@method.
- endpoint

```

## Logback config

The following is the appender defined in the logback.xml or logback-test.xml

```
    <!--audit log-->
    <appender name="audit" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>target/audit.log</file> <!-- logfile location -->
        <encoder>
            <pattern>%-5level [%thread] %date{ISO8601} %F:%L - %msg%n
            </pattern> <!-- the layout pattern used to format log entries -->
            <immediateFlush>true</immediateFlush>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.FixedWindowRollingPolicy">
            <fileNamePattern>target/audit.log.%i.zip</fileNamePattern>
            <minIndex>1</minIndex>
            <maxIndex>5</maxIndex> <!-- max number of archived logs that are kept -->
        </rollingPolicy>
        <triggeringPolicy class="ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy">
            <maxFileSize>200MB
            </maxFileSize> <!-- The size of the logfile that triggers a switch to a new logfile, and the current one archived -->
        </triggeringPolicy>
    </appender>

```

## Logging example

```
INFO  [XNIO-1 I/O-1] 2017-05-08 19:32:33,975 AuditHandler.java:141 - {"timestamp":1494286353929,"X-Correlation-Id":"abS_cAyTT5SIrHayM-11pQ","X-Traceability-Id":null,"statusCode":200,"responseTime":16}
INFO  [XNIO-1 I/O-3] 2017-05-08 19:32:33,975 AuditHandler.java:141 - {"timestamp":1494286353960,"X-Correlation-Id":"FUD_bbFpRpSs2CmVjJYt-A","X-Traceability-Id":"tid","statusCode":200,"responseTime":1}
```


## Customized Handler
For some users that need special audit logic or other channel to redirect the audit
to, they can create their own audit handler and replace the default audit handler in
/src/main/resources/META-INF/services/com.networknt.handler.MiddlewareHandler

