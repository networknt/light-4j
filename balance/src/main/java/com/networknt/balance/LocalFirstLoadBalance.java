package com.networknt.balance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.networknt.utility.Util;

import java.net.InetAddress;
import java.net.URL;
import java.util.List;

/**
 * Created by dan on 2016-12-29
 */
public class LocalFirstLoadBalance extends RoundRobinLoadBalance {
    static Logger logger = LoggerFactory.getLogger(LocalFirstLoadBalance.class);

    static String hostname = "value not assigned";
    
    static{
    	// get the address of the local host
    	// in case of Docker it will be the container name
        InetAddress inetAddress = Util.getInetAddress();
        // get hostname for this IP address
        hostname = inetAddress.getHostName();
    }
    
    protected URL doSelect(List<URL> urls) {
    	// search for a URL in the same host first
    	for(URL url : urls){
    		if(url.getHost().equalsIgnoreCase(hostname))
    			return url;
    	};

    	// URL not found in the same host, use round-robin for the next URL to access
    	return super.doSelect(urls);
    }
}
