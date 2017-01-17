package com.networknt.consul;

import com.networknt.registry.Registry;
import com.networknt.consul.client.ConsulEcwidClient;
import com.networknt.consul.client.ConsulClient;
import com.networknt.registry.support.AbstractRegistryFactory;
import com.networknt.registry.URL;
import org.apache.commons.lang3.StringUtils;


public class ConsulRegistryFactory extends AbstractRegistryFactory {

    @Override
    protected Registry createRegistry(URL url) {
        String host = ConsulConstants.DEFAULT_HOST;
        int port = ConsulConstants.DEFAULT_PORT;
        if (StringUtils.isNotBlank(url.getHost())) {
            host = url.getHost();
        }
        if (url.getPort() > 0) {
            port = url.getPort();
        }
        // can use another client implementation
        ConsulClient client = new ConsulEcwidClient(host, port);
        return new ConsulRegistry(url, client);
    }

}
