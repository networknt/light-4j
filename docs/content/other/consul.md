---
date: 2017-02-06T21:33:14-05:00
title: Consul
---

A consul registry implementation that uses Consul as registry and discovery
server. It implements both registry and discovery in the same module for
consul communication. If the API/service is delivered as docker image, another
product called registrator will be used to register it with Consul agent.
Otherwise, the server module will be responsible to register itself during
startup.

The reason that service itself in Docker cannot register to Consul is due to
the containerized service cannot find the exposed port from Docker and this
issue has been opened for a long time and it has never been resolved. 

Here is the open issue on docker github repo.

https://github.com/moby/moby/issues/3778

## Interface

Here is the interface of Consul client. 

```
public interface ConsulClient {

	/**
	 * Set specific serviceId status as pass
	 *
	 * @param serviceId service id
	 */
	void checkPass(String serviceId);

	/**
	 * Set specific serviceId status as fail
	 *
	 * @param serviceId service id
	 */
	void checkFail(String serviceId);

	/**
	 * register a consul service
	 *
	 * @param service service object
	 */
	void registerService(ConsulService service);

	/**
	 * unregister a consul service
	 *
	 * @param serviceid service id
	 */
	void unregisterService(String serviceid);

	/**
	 * get latest service list
	 *
	 * @param serviceName service name
	 * @param lastConsulIndex last consul index
	 * @return T
	 */
	ConsulResponse<List<ConsulService>> lookupHealthService(
			String serviceName, long lastConsulIndex);

	String lookupCommand(String group);

}

```

## Implementation

The implementation is based on ecwid Consul client which is an open source library
of Consul client.

## Configuration

There is no specific config file for Consul module as it will utilize service.yml

Here is an example of service.yml in test folder for Consul module to define that
Mocked Consul client is used for ConsulClient interface.

```
description: singleton service factory configuration
singletons:
- com.networknt.registry.URL:
  - com.networknt.registry.URLImpl:
      protocol: light
      host: localhost
      port: 8500
      path: ''
      parameters:
        registrySessionTimeout: '1000'
- com.networknt.consul.client.ConsulClient:
  - com.networknt.consul.MockConsulClient:
    - java.lang.String: localhost
    - int: 8500
- com.networknt.registry.Registry:
  - com.networknt.consul.ConsulRegistry

```
