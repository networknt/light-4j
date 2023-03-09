package com.networknt.client.simplepool.mock;

import com.networknt.client.simplepool.SimpleConnectionHolder;
import com.networknt.client.simplepool.SimpleConnectionMaker;
import com.networknt.client.simplepool.SimpleURIConnectionPool;
import com.networknt.client.simplepool.mock.mockexample.MockKeepAliveConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public class TestRunner
{
    private static final Logger logger = LoggerFactory.getLogger(TestRunner.class);

    // Default Test Runner Settings
    private long testLength = 120;      // in seconds
    private int numCallers = 2;

    // Default Connection Pool Setup
    private URI uri = URI.create("https://mock-uri.com");
    private long expireTime = 10;      // in seconds
    private int poolSize = 100;
    private Class connectionClass;
    private SimpleConnectionMaker connectionMaker;
    private SimpleURIConnectionPool pool;

    // Caller Thread setup
    private long createConnectionTimeout = 5; // in seconds
    private long borrowTime = 3;              // in seconds
    private long borrowJitter = 4;            // in seconds
    private long reconnectTime = 2;           // in seconds
    private long reconnectTimeJitter = 2;     // in seconds
    private int threadStartJitter = 3;        // in seconds
    private boolean isHttp2 = true;

    /** Test length in seconds. Default 120s */
    public TestRunner setTestLength(long testLength) { this.testLength = testLength; return this; }
    /** Number of borrowing threads. Default 2 */
    public TestRunner setNumCallers(int numCallers) { this.numCallers = numCallers; return this; }
    /** Mock URI. Default https://mock-uri.com */
    public TestRunner setUri(URI uri) { this.uri = uri; return this; }
    public TestRunner setConnectionPoolSize(int poolSize) { this.poolSize = poolSize; return this; }
    /** Connection expiry time in seconds. Default 10s */
    public TestRunner setExpireTime(long expireTime) { this.expireTime = expireTime; return this; }
    /** Maximum pool size. Default 100 */
    public TestRunner setPoolSize(int poolSize) { this.poolSize = poolSize; return this; }
    /** The SimpleConnection class used for connections -- must have a parameterless constructor. Default is SimpleForeverConnection */
    public TestRunner setConnectionClass(Class connectionClass) { this.connectionClass = connectionClass; return this; }
    /** Connection creation timeout in seconds. Default is 5s */
    public TestRunner setCreateConnectionTimeout(long createConnectionTimeout) { this.createConnectionTimeout = createConnectionTimeout; return this; }
    /** Amount of time in seconds that borrower threads hold connections before restoring them. Default 3s */
    public TestRunner setBorrowTime(long borrowTime) { this.borrowTime = borrowTime; return this; }
    /** Max random additional time in seconds that borrower threads hold connections before restoring them. Default 4s */
    public TestRunner setBorrowTimeJitter(long borrowJitter) { this.borrowJitter = borrowJitter; return this; }
    /** Amount of time in seconds that borrower threads waits after returning a connection to borrow again. Default 2s */
    public TestRunner setReconnectWaitTime(long reconnectTime) { this.reconnectTime = reconnectTime; return this; }
    /** Max random additional time in seconds that borrower threads waits after returning a connection to borrow again. Default 2s */
    public TestRunner setReconnectWaitTimeJitter(long reconnectTimeJitter) { this.reconnectTimeJitter = reconnectTimeJitter; return this; }
    /** Max random startup delay in seconds for borrower threads. Default 3s */
    public TestRunner setThreadStartJitter(int threadStartJitter) { this.threadStartJitter = threadStartJitter; return this; }
    /** Determines whether caller threads request HTTP/2 connections. HTTP/2 means multiple borrows per connection are allowed. Default true */
    public TestRunner setHttp2(boolean http2) { isHttp2 = http2; return this; }

    public void executeTest() {
        try {
            // create connection maker
            connectionMaker = new TestConnectionMaker(connectionClass);
            // create pool
            pool = new SimpleURIConnectionPool(uri, expireTime * 1000, poolSize, connectionMaker);

            // flag used to stop threads
            AtomicBoolean stopped = new AtomicBoolean(false);
            CountDownLatch latch = new CountDownLatch(numCallers);

            logger.debug("> Creating and starting threads...");
            createAndStartCallers(
                    numCallers, threadStartJitter, pool, stopped, createConnectionTimeout, isHttp2, borrowTime, borrowJitter, reconnectTime, reconnectTimeJitter, latch);
            logger.debug("> All threads created and started");

            logger.debug("> SLEEP for {} seconds", testLength);
            Thread.sleep(testLength * 1000);

            logger.debug("> WAKE");
            logger.debug("> Shutting down test...");
            stopped.set(true);

            logger.debug("> Thread-shutdown flag set. Waiting for threads to exit...");
            latch.await();

            logger.debug("> Threads exited. Test completed");
        } catch (Exception e) {
            logger.debug("> Test had errors", e);
        }
    }

    private void createAndStartCallers(
            int numCallers,
            int threadStartJitter,
            SimpleURIConnectionPool pool,
            AtomicBoolean stopped,
            long createConnectionTimeout,
            boolean isHttp2,
            long borrowTime,
            long borrowJitter,
            long reconnectTime,
            long reconnectTimeJitter,
            CountDownLatch latch) throws InterruptedException
    {
        while(numCallers-- > 0) {
            new CallerThread(
                pool, stopped, createConnectionTimeout, isHttp2, borrowTime, borrowJitter, reconnectTime, reconnectTimeJitter, latch).start();
            if(threadStartJitter > 0)
                Thread.sleep(ThreadLocalRandom.current().nextLong(threadStartJitter+1) * 1000);
        }
    }

    private static class CallerThread extends Thread {
        private static final Logger logger = LoggerFactory.getLogger(CallerThread.class);
        private CountDownLatch latch;
        private AtomicBoolean stopped;
        private SimpleURIConnectionPool pool;
        private long createConnectionTimeout;
        private boolean isHttp2;
        private long borrowTime;
        private long borrowJitter;
        private long reconnectTime;
        private long reconnectTimeJitter;

        public CallerThread(
                SimpleURIConnectionPool pool,
                AtomicBoolean stopped,
                long createConnectionTimeout,
                boolean isHttp2,
                long borrowTime,
                long borrowJitter,
                long reconnectTime,
                long reconnectTimeJitter,
                CountDownLatch latch)
        {
            this.latch = latch;
            this.stopped = stopped;
            this.pool = pool;
            this.createConnectionTimeout = createConnectionTimeout; // this must be kept in seconds (not ms)
            this.isHttp2 = isHttp2;
            this.borrowTime = borrowTime;
            this.borrowJitter = borrowJitter;
            this.reconnectTime = reconnectTime;
            this.reconnectTimeJitter = reconnectTimeJitter;
        }

        @Override
        public void run() {
            logger.debug("{} Starting", Thread.currentThread().getName());
            while(!stopped.get()) {
                SimpleConnectionHolder.ConnectionToken connectionToken = null;
                try {
                    logger.debug("{} Borrowing connection", Thread.currentThread().getName());
                    connectionToken = pool.borrow(createConnectionTimeout, isHttp2);

                } catch(Exception e) {
                    logger.debug("{} Connection issue occurred!", Thread.currentThread().getName(), e);

                } finally {
                    if(connectionToken != null)
                        borrowTime(borrowTime, borrowJitter);

                    logger.debug("{} Returning connection", Thread.currentThread().getName());
                    pool.restore(connectionToken);

                    reborrowWaitTime(reconnectTime, reconnectTimeJitter);
                }
            }
            latch.countDown();
            logger.debug("{} Thread exiting", Thread.currentThread().getName());
        }

        private void borrowTime(long borrowTime, long borrowJitter) {
            long borrowTimeMs = borrowTime * 1000;
            long borrowJitterMs = borrowJitter * 1000;
            try {
                final long randomBorrowJitterMs;
                if (borrowJitterMs > 0)
                    randomBorrowJitterMs = ThreadLocalRandom.current().nextLong(borrowJitterMs + 1);
                else
                    randomBorrowJitterMs = 0;
                logger.debug("{} Using connection for {} seconds...", Thread.currentThread().getName(), (borrowTimeMs + randomBorrowJitterMs)/1000);
                Thread.sleep(borrowTimeMs + randomBorrowJitterMs);
            } catch(InterruptedException e) {
                logger.debug("Thread interrupted during borrowDelay()", e);
            }
        }

        private void reborrowWaitTime(long reconnectTime, long reconnectTimeJitter) {
            long reconnectTimeMs = reconnectTime * 1000;
            long reconnectTimeJitterMs = reconnectTimeJitter * 1000;
            try {
                final long randomReconnectJitterMs;
                if (reconnectTimeJitterMs > 0)
                    randomReconnectJitterMs = ThreadLocalRandom.current().nextLong(reconnectTimeJitterMs + 1);
                else
                    randomReconnectJitterMs = 0;
                logger.debug("{} Waiting for {} seconds to borrow connection again...", Thread.currentThread().getName(), (reconnectTimeMs + randomReconnectJitterMs)/1000);
                Thread.sleep(reconnectTimeMs + randomReconnectJitterMs);
            } catch(InterruptedException e) {
                logger.debug("Thread interrupted during reborrowWaitTime()", e);
            }
        }
    }
}
