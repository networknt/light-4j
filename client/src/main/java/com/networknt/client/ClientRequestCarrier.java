package com.networknt.client;

import io.undertow.client.ClientRequest;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;

public class ClientRequestCarrier implements io.opentracing.propagation.TextMap {
    private final static Logger logger = LoggerFactory.getLogger(ClientRequestCarrier.class);
    private final ClientRequest clientRequest;

    ClientRequestCarrier(ClientRequest clientRequest) {
        this.clientRequest = clientRequest;
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        throw new UnsupportedOperationException("carrier is write-only");
    }

    @Override
    public void put(String key, String value) {
        if(logger.isDebugEnabled()) logger.debug("key = " + key + " value = " + value);
        clientRequest.getRequestHeaders().put(new HttpString(key), value);
    }
}
