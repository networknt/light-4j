## egress-router module

The module provides router (egress traffic) related handlers and util classes.

 

When the system need call light-api from legacy system or API developed different technologies, for example, node.js, .net, the client side need process service discovery, JWT token management, load TLS certs...;

Those client module feature can be done by using router handlers. The router handlers can delegate the client module features and help client side application to process service discovery, JWT token management, load TLS certs, etc.

  


### To learn how to use light-router, pleases refer to

* [Light-router](https://github.com/networknt/light-router) github repo

* [Getting Started](https://www.networknt.com/getting-started/light-router/) to learn core concepts
* [Tutorial](https://www.networknt.com/tutorial/router/) with step by step guide for RESTful proxy
* [Configuration](https://www.networknt.com/service/router/configuration/) for different configurations based on your situations
