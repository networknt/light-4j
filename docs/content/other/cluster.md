---
date: 2017-02-06T21:33:04-05:00
title: Cluster
---

This module caches all the service instances that are needed by the current 
service and calling underline registry(Direct, Consul and ZooKeeper) to
discover the service if necessary (first time a service is called and registry
notifies something has been changed regarding to subscribe the services on
Consul or ZooKeeper).

## Interface

In this module, we have an interface called Cluster.java

```
public interface Cluster {
    /**
     * give a service name and return a url with http or https url
     * the result is has been gone through the load balance with request key
     *
     * requestKey is used to control the behavior of load balance except
     * round robin and local first which this value is null. For consistent hash
     * load balance, normally client_id or user_id from JWT token should be passed
     * in to route the same client to the same server all the time or the same user
     * to the same server all the time
     */
    String serviceToUrl(String protocol, String serviceId, String requestKey);
}
```

## Implementation

LightCluster is a default implementation for the above interface. It integrates
Service discovery, notification and load balance together to facilitate service
lookup during runtime. 

## Configuration

There is no specific config file for cluster but only service.yml to control
which implementation to be loaded during server startup. Here is an example
configuration in the test case. 

```
description: singleton service factory configuration
singletons:
- com.networknt.registry.URL:
  - com.networknt.registry.URLImpl:
      protocol: https
      host: localhost
      port: 8080
      path: direct
      parameters:
        com.networknt.apib-1.0.0: http://localhost:7002,http://localhost:7005
        com.networknt.apic-1.0.0: http://localhost:7003
- com.networknt.registry.Registry:
  - com.networknt.registry.support.DirectRegistry
- com.networknt.balance.LoadBalance:
  - com.networknt.balance.RoundRobinLoadBalance
- com.networknt.cluster.Cluster:
  - com.networknt.cluster.LightCluster

```

The above service.yml defines DirectRegistry, RoundRobinLoadBalance and LightCluster
as implementations for corresponding interfaces. 

