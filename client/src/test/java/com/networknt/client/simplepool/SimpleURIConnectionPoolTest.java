package com.networknt.client.simplepool;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SimpleURIConnectionPoolTest {

    private SimpleURIConnectionPool pool;
    private URI uri = URI.create("https://localhost:8443");
    private long expireTime = 10000; // 10 seconds
    private int poolSize = 5;
    
    @Mock
    private SimpleConnectionMaker connectionMaker;

    @Mock
    private SimpleConnection mockConnection;

    @Before
    public void setUp() {
        // Default mock behavior for connection
        when(mockConnection.isOpen()).thenReturn(true);
        when(mockConnection.isMultiplexingSupported()).thenReturn(false); // HTTP/1.1 behavior for deterministic counting
        when(mockConnection.getLocalAddress()).thenReturn("localhost:12345");
        
        // Mock the makeConnection method used by SimpleConnectionHolder constructor
        // simple signature: makeConnection(long, InetSocketAddress, URI, XnioWorker, XnioSsl, ByteBufferPool, OptionMap, Set)
        when(connectionMaker.makeConnection(
                anyLong(), 
                nullable(java.net.InetSocketAddress.class), 
                eq(uri), 
                nullable(org.xnio.XnioWorker.class), 
                nullable(org.xnio.ssl.XnioSsl.class), 
                nullable(io.undertow.connector.ByteBufferPool.class), 
                nullable(org.xnio.OptionMap.class), 
                anySet()))
            .thenReturn(mockConnection);
            
        // Mock reuseConnection to return the same connection by default
        when(connectionMaker.reuseConnection(anyLong(), any(SimpleConnection.class)))
            .thenAnswer(invocation -> invocation.getArgument(1));
            
        pool = new SimpleURIConnectionPool(uri, expireTime, poolSize, connectionMaker);
    }

    @Test
    public void testBorrowAndRestore() {
        // Borrow a connection
        SimpleConnectionHolder.ConnectionToken token = pool.borrow(1000);
        Assert.assertNotNull("Token should not be null", token);
        Assert.assertNotNull("Connection should not be null", token.connection());
        Assert.assertEquals("Borrowed count should be 1", 1, pool.getBorrowedCount());
        Assert.assertEquals("Borrowable count should be 0", 0, pool.getBorrowableCount());

        // Restore the connection
        pool.restore(token);
        Assert.assertEquals("Borrowed count should be 0", 0, pool.getBorrowedCount());
        Assert.assertEquals("Borrowable count should be 1", 1, pool.getBorrowableCount());
    }

    @Test
    public void testPoolSizeLimit() {
        // Create a smaller pool for this test
        pool = new SimpleURIConnectionPool(uri, expireTime, 2, connectionMaker);

        // Borrow 2 connections (filling the pool)
        pool.borrow(1000);
        pool.borrow(1000);
        Assert.assertEquals("Borrowed count should be 2", 2, pool.getBorrowedCount());

        // Attempt to borrow a 3rd should fail
        try {
            pool.borrow(1000);
            Assert.fail("Should have thrown RuntimeException for pool size limit");
        } catch (RuntimeException e) {
            Assert.assertTrue("Exception message should check pool size", 
                e.getMessage().contains("exceed the maximum size"));
        }
    }

    @Test
    public void testReuseFromPool() {
        // Borrow and restore to populate pool
        SimpleConnectionHolder.ConnectionToken token1 = pool.borrow(1000);
        SimpleConnection conn1 = token1.connection();
        pool.restore(token1);

        // Borrow again - should get the same connection
        SimpleConnectionHolder.ConnectionToken token2 = pool.borrow(1000);
        Assert.assertSame("Should reuse the same connection", conn1, token2.connection());
        
        // Verify connection maker was only called once
        verify(connectionMaker, times(1)).makeConnection(
                anyLong(), 
                nullable(java.net.InetSocketAddress.class), 
                eq(uri), 
                nullable(org.xnio.XnioWorker.class), 
                nullable(org.xnio.ssl.XnioSsl.class), 
                nullable(io.undertow.connector.ByteBufferPool.class), 
                nullable(org.xnio.OptionMap.class), 
                anySet());
    }

    @Test
    public void testConnectionExpiry() throws InterruptedException {
        // Create a pool with short expiry
        long shortExpiry = 100; // 100ms
        pool = new SimpleURIConnectionPool(uri, shortExpiry, poolSize, connectionMaker);

        // Borrow and restore
        SimpleConnectionHolder.ConnectionToken token = pool.borrow(1000);
        pool.restore(token);
        Assert.assertEquals("Should have 1 borrowable connection", 1, pool.getBorrowableCount());

        // Wait for expiry
        Thread.sleep(200);

        // Trigger maintenance/borrow which checks expiry
        // The pool cleans up expired connections when borrow is called or when readAllConnectionHolders is triggered
        // borrowing again should trigger cleanup and create a new connection
        pool.borrow(1000);

        // Original connection should be closed
        verify(mockConnection, atLeastOnce()).safeClose();
        
        // Count might still be 1 because we borrowed a new one, but we expect maker to be called twice
        verify(connectionMaker, times(2)).makeConnection(
                anyLong(), 
                nullable(java.net.InetSocketAddress.class), 
                eq(uri), 
                nullable(org.xnio.XnioWorker.class), 
                nullable(org.xnio.ssl.XnioSsl.class), 
                nullable(io.undertow.connector.ByteBufferPool.class), 
                nullable(org.xnio.OptionMap.class), 
                anySet());
    }
    
    @Test
    public void testConnectionCreationFailure() {
        // Mock maker to return null (simulating failure)
        when(connectionMaker.makeConnection(
                anyLong(), 
                nullable(java.net.InetSocketAddress.class), 
                eq(uri), 
                nullable(org.xnio.XnioWorker.class), 
                nullable(org.xnio.ssl.XnioSsl.class), 
                nullable(io.undertow.connector.ByteBufferPool.class), 
                nullable(org.xnio.OptionMap.class), 
                anySet()))
            .thenReturn(null);

        // We expect a specific behavior when maker returns null - usually it throws or returns null wrapped
        // Use a try-catch connection timeout wrapper logic in pool?
        // Reading SimpleURIConnectionPool.borrow -> SimpleConnectionHolder constructor -> throws IO/RuntimeException if null?
        
        // Let's verify what happens. SimpleConnectionHolder throws if connection is null.
        
        try {
            pool.borrow(100);
            Assert.fail("Should throw exception when connection creation fails");
        } catch (Exception e) {
            // Expected
        }
    }

    @Test
    public void testLeakedConnectionCleanup() {
        // This is harder to test with Mockito purely because we need to simulate the internal state.
        // But we can verify validateAndCleanConnections calls.
        
        // Borrow and restore
        SimpleConnectionHolder.ConnectionToken token = pool.borrow(1000);
        pool.restore(token);
        
        // Mock connection as closed
        when(mockConnection.isOpen()).thenReturn(false);
        
        // Manually trigger cleanup
        int cleaned = pool.validateAndCleanConnections();
        
        // Since connection is closed, it should be removed
        Assert.assertEquals("Should have cleaned up 1 connection", 1, cleaned);
        Assert.assertEquals("Active connections should be 0", 0, pool.getActiveConnectionCount());
    }
}
