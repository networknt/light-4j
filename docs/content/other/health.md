---
date: 2017-02-06T21:33:38-05:00
title: Health Check
---

This is a server health handler that output OK to indicate the server is alive. Normally,
it will be use by F5 to check if the server is health before route request to it. Another
way to check server health is to ping the ip and port and it is the standard way to check
server status for F5. However, the service instance is up and running doesn't mean it is
functioning. This is the reason to provide a this handler to output more information about
the server for F5 or maybe in the future for the API marketplace.

Note that we only recommend to use F5 as reverse proxy for services with static IP addresses
that act like traditional web server. These services will be sitting in DMZ to serve mobile
native and browser SPA and aggregate other services in the backend. For services deployed
in the cloud dynamically, there is no reverse proxy but using client side service discovery.

This is an handler that needs to be injected into the request/response chain in order to 
return something that indicate the server is still alive. Currently it returns "OK" only and
in the future, it will be enhanced to add more features.


## Configuration

Here is the default config for the module.

```
# Server health endpoint that can output OK to indicate the server is alive

# true to enable this middleware handler.
enabled: true
```
