package com.networknt.cluster;

/**
 * Created by stevehu on 2017-01-27.
 */
public interface Cluster {
    // give a service name and return a url with http or https url
    // the result is has been gone through the load balance.
    String serviceToUrl(String protocol, String serviceId);
}
