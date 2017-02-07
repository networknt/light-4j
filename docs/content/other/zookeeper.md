---
date: 2017-02-06T21:34:10-05:00
title: Zookeeper
---

A Zookeeper registry implementation that use Zookeeper as registry and discovery
server. It implements both registry and discovery in the same module for
Zookeeper communication. If the API/server is delivered as docker image, another
product called registrator will be used to register it with Zookeeper server.
Otherwise, the server module will be responsible to register itself during
startup.