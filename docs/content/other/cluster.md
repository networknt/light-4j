---
date: 2017-02-06T21:33:04-05:00
title: Cluster
---

This module caches all the service instances that are needed by the current 
service and calling underline registry(Direct, Consul and ZooKeeper) to
discover the service if necessary (first time a service is called and registry
notifies something has been changed regarding to subscribe the services on
Consul or ZooKeeper).

