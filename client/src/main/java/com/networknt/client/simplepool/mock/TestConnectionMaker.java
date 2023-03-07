package com.networknt.client.simplepool.mock;

import com.networknt.client.simplepool.SimpleConnection;
import com.networknt.client.simplepool.SimpleConnectionMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.*;

public class TestConnectionMaker implements SimpleConnectionMaker {
    private static final Logger logger = LoggerFactory.getLogger(TestConnectionMaker.class);
    private Class connectionClass;
    public TestConnectionMaker(Class clas) {
        this.connectionClass = clas;
    }

    @Override
    public SimpleConnection makeConnection(long createConnectionTimeout, boolean isHttp2, URI uri, Set<SimpleConnection> allConnections)
        throws RuntimeException
    {
        SimpleConnection connection = instantiateConnection(createConnectionTimeout, isHttp2, allConnections);
        return connection;
    }

    @Override
    public SimpleConnection reuseConnection(long createConnectionTimeout, SimpleConnection connection) throws RuntimeException {
        if(connection == null)
            return null;
        if(!connection.isOpen())
            throw new RuntimeException("Reused-connection has been unexpectedly closed");
        return connection;
    }

    private SimpleConnection instantiateConnection(long createConnectionTimeout, final boolean isHttp2, final Set<SimpleConnection> allConnections)
            throws RuntimeException
    {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<SimpleConnection> future = executor.submit(() -> {
            executor.shutdown();

            Constructor<SimpleConnection> constructor = connectionClass.getConstructor(boolean.class);
            SimpleConnection simpleConnection = constructor.newInstance(isHttp2);

            allConnections.add(simpleConnection);
            logger.debug("allCreatedConnections: {}", logAllConnections(allConnections));

            return simpleConnection;
        });
        SimpleConnection connection;
        try {
            connection = future.get(createConnectionTimeout, TimeUnit.SECONDS);
        } catch(Exception e) {
            throw new RuntimeException("Connection creation timed-out");
        }
        return connection;
    }


    /***
     * For logging
     */
    private String logAllConnections(final Set<SimpleConnection> allConnections) {
        StringBuilder consList = new StringBuilder();
        consList.append("[ ");
        for(SimpleConnection connection: allConnections)
            consList.append(port(connection)).append(" ");
        consList.append("]:");
        return consList.toString();
    }

    /***
     * For logging
     */
    private static String port(SimpleConnection connection) {
        if(connection == null) return "NULL";
        String url = connection.getLocalAddress();
        int semiColon = url.lastIndexOf(":");
        if(semiColon == - 1) return "PORT?";
        return url.substring(url.lastIndexOf(":")+1);
    }
}
