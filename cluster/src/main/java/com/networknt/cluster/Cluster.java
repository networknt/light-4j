package com.networknt.cluster;

/**
 * Cluster interface is used to lookup a service instance by protocol, service id
 * and requestKey if necessary. Under the hood, it calls load balance to pick up
 * an instance from multiple instances retrieved from client side service discovery.
 *
 * Created by stevehu on 2017-01-27.
 */
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
     *
     * @param protocol either http or https
     * @param serviceId unique service identifier
     * @param tag an environment tag use along with serviceId for discovery
     * @param requestKey load balancer key
     * @return String url
     */
    String serviceToUrl(String protocol, String serviceId, String tag, String requestKey);
}
