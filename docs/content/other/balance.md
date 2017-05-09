---
date: 2017-02-06T21:32:51-05:00
title: Load Balance
---

Light-4j framework encourages client side discovery in order to avoid any proxy in
front of multiple instances of services. This can reduce the network hop and 
subsequently reduce the latency of service call. 
 
Client side discovery needs client side load balancer in order to pick up one and 
only one available service instance from a list of available services for a 
particular downstream request during runtime. 

Currently, Round-Robin and LocalFirst are implemented and ConsistentHashing is in
progress. By default, round-robin will be used and it gives all service instances
equally opportunity to be invoked. 


## LoadBalance interface

All load balance will be implementing LoadBalance interface.

```
public interface LoadBalance {
    // select one from a list of URLs

    /**
     * Select one url from a list of url with requestKey as optional.
     *
     * @param urls List
     * @param requestKey String
     * @return URL
     */
    URL select(List<URL> urls, String requestKey);

    /**
     * return positive int value of originValue
     * @param originValue original value
     * @return positive int
     */
    default int getPositive(int originValue){
        return 0x7fffffff & originValue;
    }

}

```

## RoundRobinLoadBalance

Round Robin Loadbalance will pick up a url from a list of urls one by one
for each service call. It will distributed the load equally to all urls in the list.

This class has an instance variable called idx which is AtomicInteger and it
increases for every select call to make sure all urls in the list will have
an opportunity to be selected.

The assumption for round robin is based on all service will have the same
hardware/cloud resource configuration so that they can be treated as the
same priority without any weight. 
 
Round robin requestKey is not used as it should be null, the url will
be selected from the list base on an instance idx. 
 
 
## LocalFirstLoadBalance

Local first load balance give local service high priority than remote services.
If there is no local service available, then it will adapt round robin strategy.

With all the services in the list of urls, find local services with IP, Chances are
we have multiple local service, then round robin will be used in this case. If
there is no local service, find the first remote service according to round robin.

Local first requestKey is not used as it is ip on the localhost. It first needs to
find a list of urls on the localhost for the service, and then round robin in the
list to pick up one.

Currently, this load balance is only used if you deploy the service as standalone
java process on data center hosts. We need to find a way to identify two VMs or two
docker containers sitting on the same physical machine in the future to improve this
load balance.

It is also suitable if your services are built on top of [light-hybrid-4j](https://github.com/networknt/light-hybrid-4j) 
and want to use the remote interface for service to service communication.


## ConsistentHashLoadBalance

To obtain maximum scalability, microservices allow Y-Axis scale to break up big
monolithic application to small functional units. However, for some of the heavy
load services, we can use data sharding for Z-Axis scale. This load balance is
designed for that.

In normal case, the requestKey should be client_id or user_id from JWT token, this
can guarantee that the same client will always to be routed to one service instance
or one user always to be routed to one service instance. However, this key can be a
combination of multiple fields from the request.

This load balance is not completed yet. There are some articles talking about this
on the Internet but majority of the implementations suffers imbalance because of
random nature of hashing. 

Here is a paper that resolve the issue. 

```
https://medium.com/vimeo-engineering-blog/improving-load-balancing-with-a-new-consistent-hashing-algorithm-9f1bd75709ed
https://arxiv.org/abs/1608.01350
```

## Configuration

We have several load balance implementations available, there is only one should
be used for one client during runtime and it is controlled by service.yml config
file. 

Here is an example that use RoundRobinLoadBalance. 

```
# Default load balance is round robin and it can be replace with another implementation.
---
description: singleton service factory configuration
singletons:
- com.networknt.balance.LoadBalance:
  - com.networknt.balance.RoundRobinLoadBalance
```
