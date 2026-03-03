package com.networknt.client.simplepool;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SimpleURIConnectionPoolTest {

    private SimpleURIConnectionPool pool;
    private URI uri = URI.create("https://localhost:8443");
    private long expireTime = 10000; // 10 seconds
    private int poolSize = 5;

    @Mock
    private SimpleConnectionMaker connectionMaker;

    @Mock
    private SimpleConnection mockConnection;

    @BeforeEach
    public void setUp() {
        // Default mock behavior for connection
        when(mockConnection.isOpen()).thenReturn(true);
        when(mockConnection.isMultiplexingSupported()).thenReturn(false); // HTTP/1.1 behavior for deterministic counting
        when(mockConnection.getLocalAddress()).thenReturn("localhost:12345");

        // Mock the makeConnection method used by SimpleConnectionState constructor
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


        pool = new SimpleURIConnectionPool(uri, expireTime, poolSize, connectionMaker);
    }

    @Test
    public void testBorrowAndRestore() {
        // Borrow a connection
        SimpleConnectionState.ConnectionToken token = pool.borrow(1000);
        Assertions.assertNotNull(token, "Token should not be null");
        Assertions.assertNotNull(token.connection(), "Connection should not be null");
        Assertions.assertEquals(1, pool.getBorrowedCount(), "Borrowed count should be 1");
        Assertions.assertEquals(0, pool.getBorrowableCount(), "Borrowable count should be 0");

        // Restore the connection
        pool.restore(token);
        Assertions.assertEquals(0, pool.getBorrowedCount(), "Borrowed count should be 0");
        Assertions.assertEquals(1, pool.getBorrowableCount(), "Borrowable count should be 1");
    }

    @Test
    public void testPoolSizeLimit() {
        // Create a smaller pool for this test
        pool = new SimpleURIConnectionPool(uri, expireTime, 2, connectionMaker);

        // Borrow 2 connections (filling the pool)
        pool.borrow(1000);
        pool.borrow(1000);
        Assertions.assertEquals(2, pool.getBorrowedCount(), "Borrowed count should be 2");

        // Attempt to borrow a 3rd should fail
        try {
            pool.borrow(1000);
            Assertions.fail("Should have thrown RuntimeException for pool size limit");
        } catch (RuntimeException e) {
            Assertions.assertTrue(e.getMessage().contains("exceed the maximum size"), "Exception message should check pool size");
        }
    }

    @Test
    public void testReuseFromPool() {
        // Borrow and restore to populate pool
        SimpleConnectionState.ConnectionToken token1 = pool.borrow(1000);
        SimpleConnection conn1 = token1.connection();
        pool.restore(token1);

        // Borrow again - should get the same connection
        SimpleConnectionState.ConnectionToken token2 = pool.borrow(1000);
        Assertions.assertSame(conn1, token2.connection(), "Should reuse the same connection");

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
        SimpleConnectionState.ConnectionToken token = pool.borrow(1000);
        pool.restore(token);
        Assertions.assertEquals(1, pool.getBorrowableCount(), "Should have 1 borrowable connection");

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
        // Reading SimpleURIConnectionPool.borrow -> SimpleConnectionState constructor -> throws IO/RuntimeException if null?

        // Let's verify what happens. SimpleConnectionState throws if connection is null.

        try {
            pool.borrow(100);
            Assertions.fail("Should throw exception when connection creation fails");
        } catch (Exception e) {
            // Expected
        }
    }

    @Test
    public void testLeakedConnectionCleanup() {
        // This is harder to test with Mockito purely because we need to simulate the internal state.
        // But we can verify validateAndCleanConnections calls.

        // Borrow and restore
        SimpleConnectionState.ConnectionToken token = pool.borrow(1000);
        pool.restore(token);

        // Mock connection as closed
        when(mockConnection.isOpen()).thenReturn(false);

        // Manually trigger cleanup
        int cleaned = pool.validateAndCleanConnections();

        // Since connection is closed, it should be removed
        Assertions.assertEquals(1, cleaned, "Should have cleaned up 1 connection");
        Assertions.assertEquals(0, pool.getActiveConnectionCount(), "Active connections should be 0");
    }
}
