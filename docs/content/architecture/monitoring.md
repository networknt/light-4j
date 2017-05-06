---
date: 2016-11-09T21:13:27-05:00
title: Monitoring
---

Monitoring used to be a somewhat passive thing. You used tools to monitor the 
application process/logs and perhaps send an alert if something seemed wrong,  
but mostly it was hands off. When we move to microservices architecture, things
are changing.  

## User Experience and Microservices Monitoring

With Microservices which are released more often, you can try new features and
see how they impact user usage patterns. With this feedback, you can improve 
your application. It is not uncommon to employ A/B testing and multi-variant 
testing to try out new combinations of features. Monitoring is more than just 
watching for failure. With big data, data science, and microservices, 
monitoring microservices runtime stats is required to know your application 
users. You want to know what your users like and dislike and react. 

## Debugging and Microservices Monitoring 

Runtime statistics and metrics are critical for distributed systems. Since 
microservices architecture use a lot of remote calls. Monitoring microservices 
metrics can include request per second, available memory, #threads, #connections, 
failed authentication, expired tokens, etc. These parameters are important for 
understanding and debugging your code. Working with distributed systems is hard. 
Working with distributed systems without reactive monitoring is crazy. Reactive 
monitoring allows you to react to failure conditions and ramp of services for 
higher loads.

## Circuit Breaker and Microservices Monitoring
 
You can employ the Circuit Breaker pattern to prevent a catastrophic cascade, 
and reactive microservices monitoring can be the trigger. Downstream services 
can be registered in a service discovery so that you can mark nodes as unhealthy 
as well react by reroute in the case of outages. The reaction can be serving up 
a deprecated version of the data or service, but the key is to avoid cascading 
failure. You don't want your services falling over like dominoes.

## Cloud Orchestration and Microservices Monitoring 

Reactive microservices monitoring would enable you to detect heavy load, and 
spin up new instances with the cloud orchestration platform of your choice 
(Kubernetes, EC2, CloudStack, OpenStack, Rackspace etc.). 

## Public Microservices and Microservices Monitoring

Microservices monitoring of runtime statistics can be used to rate limiting 
a partners Application Id/client Id. You don't want partners to consume all of your 
well-tuned, high-performant microservices resources. It is okay to trust your 
partners but use Microservices Monitoring to verify. 

Monitoring public microservices is your way to verify. Once you make microservices 
publicly available or partner available, you have to monitor and rate limit. 

This is not a new concept. If you have ever used a public REST API from Google for 
example, you are well aware of rate limiting. A rate limit will do things like limit 
the number of connections you’re allowed to make. It is common for rate limits to 
limit the number of certain requests that a client id or partner id is allowed to 
make in a given time period. This is protection. 

Deploying public or partner accessible microservices without this protection is 
lunacy and a recipe for disaster, unless you like failing when someone decides to 
hit your endpoints 10x more than you did the capacity planning for. Avoid long 
nights and tears. Monitor microservices that you publish, and limit access to them.


## Microservices Framework and Microservices Monitoring

Light-4J a mircoservices framework that comes with a runtime metrics which can 
be used for Microservices Monitoring. 

* You can query /server/health endpoint to detect if the service is available and healthy.
* The framework collects metrics info and pushes it into influxdb and dashboard can be viewed from Grafana.
* Rate limiting can be enabled at client_id level or ip address/user level.
* Kubernetes monitors load of each pods and can start new instances on demand.
* TraceabilityId and CorrelationId in logs that can be traced with tools like Logstash, GrayLog and Splunk.
* Specifically designed error code can be monitored and send alert if some of them shown up in logs.

## Reactive Microservices Monitoring

Reactive Microservices Monitoring is an essential ingredient of microservices architecture. 
You need it for debugging, knowing your users, working with partners, building reactive 
systems that react to load and failures without cascading outages. Reactive Microservices 
Monitoring can not be a hindsight decision. Build your microservices with microservices 
monitoring in mind from the start. Make sure that the microservices lib that you use has 
monitoring of runtime statistics built in from the start. Make sure that is a core part of 
the microservices library. Code Hale Statistics allow you to gather metrics in 
a standard way. Tools like Influxdb and Grafana, Kibana help you understand the 
data, and build dashboards. Light 4J, the Java Microservices Framework, includes a metrics 
middleware which feeds into CodeHale Metrics. Light 4J also proivdes a rate limiting 
middleware to limit access per client_id or IP address/user. The container orchestration tool
like Kubernetes can also spin up new nodes/pods. With big data, data science, 
and microservices, monitoring microservices runtime stats is required to know your application 
users, know your partners, know what your system will do under load, etc. 

## Microservice Logging

Every instance of the service will have a unique identifier which most commonly will be
the docker container name or the hostname if not deployed in docker container. The code
to retrieve docker container name and hostname is the same. 

Along with docker container name, traceabilityId and correlationId will be logged as
context info for each logging statement. And once log files are aggregated together in
ELK, users can trace a particular transaction based on the traceabilityId or correlationId.

As microservices might be deployed across multiple geo-regions, the timestamp logged must
be UTC time so that logs can be easily ordered in the ELK. 



## Microservice Alerting

Logstash has features to send out alert when certain error code is spotted in the log files.

The framework has a component called status and it has all the errors defined in a YMAL
file which can be externalized. All the error code will be in a format ERRXXXXX and
certain error code can be setup in the alert to send out email or communicate to support
team with other channels.


