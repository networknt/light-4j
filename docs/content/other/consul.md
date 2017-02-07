---
date: 2017-02-06T21:33:14-05:00
title: Consul
---

A consul registry implementation that use Consul as registry and discovery
server. It implements both registry and discovery in the same module for
consul communication. If the API/server is delivered as docker image, another
product called registrator will be used to register it with Consul agent.
Otherwise, the server module will be responsible to register itself during
startup.

