/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author miklish Michael N. Christoff
 *
 * testing / QA
 *   AkashWorkGit
 *   jaydeepparekh1311
 */
package com.networknt.client.simplepool.mock;

import com.networknt.client.simplepool.SimpleConnectionState;
import com.networknt.client.simplepool.SimpleConnectionMaker;
import com.networknt.client.simplepool.SimpleURIConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TestRunner
{
    private static final Logger logger = LoggerFactory.getLogger(TestRunner.class);
    private static final Logger testThreadLogger = LoggerFactory.getLogger(CallerThread.class);

    // Default Test Runner Settings
    private long testLength = 120;      // in seconds
    private int numCallers = 2;

    // Default Connection Pool Setup
    private URI uri = URI.create("https://mock-uri.com");
    private long expireTime = 10;      // in seconds
    private int poolSize = 100;
    private Class simpleConnectionClass;
    private SimpleConnectionMaker connectionMaker;
    private SimpleURIConnectionPool pool;

    // Caller Thread setup
    private long createConnectionTimeout = 5; // in seconds
    private long borrowTime = 3;              // in seconds
    private long borrowJitter = 4;            // in seconds
    private long reborrowTime = 2;           // in seconds
    private long reborrowTimeJitter = 2;     // in seconds
    private int threadStartJitter = 3;        // in seconds
    private boolean isHttp2 = true;
    private double scheduledSafeCloseFrequency = 0.0;
    private double safeCloseFrequency = 0.0;

    private AtomicBoolean stopped = new AtomicBoolean();
    private CountDownLatch latch;

    /** Test length in seconds. Default 120s */
    public TestRunner setTestLength(long testLength) {
        this.testLength = testLength;
        return this;
    }

    /** Number of borrowing threads. Default 2 */
    public TestRunner setNumBorrowerThreads(int numCallers) {
        this.numCallers = numCallers;
        return this;
    }

    /** Mock URI. Default https://mock-uri.com */
    public TestRunner setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    /** Maximum number of connections allowed in the connection pool. Default 100 */
    public TestRunner setConnectionPoolSize(int poolSize) {
        this.poolSize = poolSize;
        return this;
    }

    /** Connection expiry time in seconds. Default 10s */
    public TestRunner setConnectionExpireTime(long expireTime) {
        this.expireTime = expireTime;
        return this;
    }

    /** The SimpleConnection class used for connections -- must have a parameterless constructor.
     *  Note: executeTest() will throw an exception if this is not set. */
    public TestRunner setSimpleConnectionClass(Class simpleConnectionClass) {
        this.simpleConnectionClass = simpleConnectionClass;
        return this;
    }

    /** Connection creation timeout in seconds. Default is 5s */
    public TestRunner setCreateConnectionTimeout(long createConnectionTimeout) {
        this.createConnectionTimeout = createConnectionTimeout;
        return this;
    }

    /** Amount of time in seconds that borrower threads hold connections before restoring them. Default 3s */
    public TestRunner setBorrowTimeLength(long borrowTime) {
        this.borrowTime = borrowTime;
        return this;
    }

    /** Max random additional time in seconds that borrower threads hold connections before restoring them. Default 4s */
    public TestRunner setBorrowTimeLengthJitter(long borrowJitter) {
        this.borrowJitter = borrowJitter;
        return this;
    }

    /** Amount of time in seconds that borrower threads waits after returning a connection to borrow again. Default 2s */
    public TestRunner setWaitTimeBeforeReborrow(long reborrowTime) {
        this.reborrowTime = reborrowTime;
        return this;
    }

    /** Max random additional time in seconds that borrower threads waits after returning a connection to borrow again. Default 2s */
    public TestRunner setWaitTimeBeforeReborrowJitter(long reborrowTimeJitter) {
        this.reborrowTimeJitter = reborrowTimeJitter;
        return this;
    }

    /** Max random startup delay in seconds for borrower threads. Default 3s */
    public TestRunner setBorrowerThreadStartJitter(int threadStartJitter) {
        this.threadStartJitter = threadStartJitter;
        return this;
    }

    /***
     * Probability between 0.0 and 1.0 that the connection will be scheduled for closure when restored. Default is 0.0.
     *
     * Note: If both 'SCHEDULED safe close frequency' and 'safe close frequency' have
     *       values above 0, then 'SCHEDULED safe close frequency' takes precedence.
     */
    public TestRunner setScheduledSafeCloseFrequency(double scheduledSafeCloseFrequency) {
        if(scheduledSafeCloseFrequency >= 0.0 && scheduledSafeCloseFrequency <= 1.0)
            this.scheduledSafeCloseFrequency = scheduledSafeCloseFrequency;
        else
            logger.error("scheduledSafeCloseFrequency must be between 0.0 and 1.0 (inclusive). Using default value of 0.0");
        return this;
    }

    /***
     * Probability between 0.0 and 1.0 that the connection will be immediately closed when restored. Default is 0.0.
     *
     * Note: If both 'SCHEDULED safe close frequency' and 'safe close frequency' have
     *       values above 0, then 'SCHEDULED safe close frequency' takes precedence.
     */
    public TestRunner setSafeCloseFrequency(double safeCloseFrequency) {
        if(safeCloseFrequency >= 0.0 && safeCloseFrequency <= 1.0)
            this.safeCloseFrequency = safeCloseFrequency;
        else
            logger.error("safeCloseFrequency must be between 0.0 and 1.0 (inclusive). Using default value of 0.0");
        return this;
    }

    /** Determines whether caller threads request HTTP/2 connections. HTTP/2 means multiple borrows per connection are allowed. Default true */
    public TestRunner setHttp2(boolean http2) {
        isHttp2 = http2;
        return this;
    }

    public void executeTest() throws RuntimeException {
        if(simpleConnectionClass == null)
            throw new RuntimeException("A SimpleConnection class must be set using setSimpleConnectionClass()");

        try {
            final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
            executor.setKeepAliveTime(Long.MAX_VALUE, TimeUnit.SECONDS);
            executor.setCorePoolSize(0);

            // create connection maker
            connectionMaker = new TestConnectionMaker(simpleConnectionClass, executor);
            // create pool
            pool = new SimpleURIConnectionPool(uri, expireTime * 1000, poolSize, connectionMaker);

            // flag used to stop threads
            stopped.set(false);
            latch = new CountDownLatch(numCallers);

            logger.debug("> Creating and starting threads...");
            createAndStartCallers();
            logger.debug("> All threads created and started");

            logger.debug("> SLEEP for {} seconds", testLength);
            Thread.sleep(testLength * 1000);

            logger.debug("> WAKE");
            logger.debug("> Shutting down test...");
            stopped.set(true);

            logger.debug("> Thread-shutdown flag set. Waiting for threads to exit...");
            latch.await();
            executor.shutdown();

            logger.debug("> Threads exited. Test completed");
        } catch (Exception e) {
            logger.debug("> Test had errors", e);
        }
    }

    private void createAndStartCallers() throws InterruptedException
    {
        while(numCallers-- > 0) {
            new CallerThread().start();
            if(threadStartJitter > 0)
                Thread.sleep(ThreadLocalRandom.current().nextLong(threadStartJitter+1) * 1000);
        }
    }

    private class CallerThread extends Thread {
        private final Logger logger;
        private final CountDownLatch latch;
        private final AtomicBoolean stopped;
        private final SimpleURIConnectionPool pool;
        private final long createConnectionTimeout;
        private final boolean isHttp2;
        private final long borrowTime;
        private final long borrowJitter;
        private final long reborrowTime;
        private final long reborrowTimeJitter;
        private final double scheduledSafeCloseFrequency;
        private final double safeCloseFrequency;

        public CallerThread() {
            this.logger = TestRunner.testThreadLogger;
            this.latch = TestRunner.this.latch;
            this.stopped = TestRunner.this.stopped;
            this.pool = TestRunner.this.pool;
            this.createConnectionTimeout = TestRunner.this.createConnectionTimeout; // this must be kept in seconds (not ms)
            this.isHttp2 = TestRunner.this.isHttp2;
            this.borrowTime = TestRunner.this.borrowTime;
            this.borrowJitter = TestRunner.this.borrowJitter;
            this.reborrowTime = TestRunner.this.reborrowTime;
            this.reborrowTimeJitter = TestRunner.this.reborrowTimeJitter;
            this.scheduledSafeCloseFrequency = TestRunner.this.scheduledSafeCloseFrequency;
            this.safeCloseFrequency = TestRunner.this.safeCloseFrequency;
        }

        @Override
        public void run() {
            logger.debug("{} Starting", Thread.currentThread().getName());
            while(!stopped.get()) {
                SimpleConnectionState.ConnectionToken connectionToken = null;
                try {
                    logger.debug("{} Borrowing connection", Thread.currentThread().getName());
                    connectionToken = pool.borrow(createConnectionTimeout, isHttp2);

                } catch(Exception e) {
                    logger.debug("{} Connection issue occurred!", Thread.currentThread().getName(), e);

                } finally {
                    if(connectionToken != null)
                        borrowTime();

                    // SCHEDULE closure
                    if(scheduledSafeCloseFrequency > 0.0 && ThreadLocalRandom.current().nextDouble() <= scheduledSafeCloseFrequency) {
                        logger.debug("{} SCHEDULING CLOSURE of connection", Thread.currentThread().getName());
                        pool.scheduleSafeClose(connectionToken);
                    }
                    // IMMEDIATELY close
                    else if (safeCloseFrequency > 0.0 && ThreadLocalRandom.current().nextDouble() <= safeCloseFrequency) {
                        logger.debug("{} IMMEDIATELY CLOSING connection", Thread.currentThread().getName());
                        pool.safeClose(connectionToken);
                    }

                    logger.debug("{} Returning connection", Thread.currentThread().getName());
                    pool.restore(connectionToken);

                    reborrowWaitTime();
                }
            }
            latch.countDown();
            logger.debug("{} Thread exiting", Thread.currentThread().getName());
        }

        private void borrowTime() {
            wait("{} Borrowing connection for {} seconds...", borrowTime, borrowJitter);
        }

        private void reborrowWaitTime() {
            wait("{} Waiting for {} seconds to borrow connection again...", reborrowTime, reborrowTimeJitter);
        }

        private void wait(String logMessage, long waitTime, long waitTimeJitter) {
            long waitTimeMs = waitTime * 1000;
            long waitTimeJitterMs = waitTimeJitter * 1000;
            try {
                final long randomReconnectJitterMs;
                if (waitTimeJitterMs > 0)
                    randomReconnectJitterMs = ThreadLocalRandom.current().nextLong(waitTimeJitterMs + 1);
                else
                    randomReconnectJitterMs = 0;
                logger.debug(logMessage, Thread.currentThread().getName(), (waitTimeMs + randomReconnectJitterMs)/1000);
                Thread.sleep(waitTimeMs + randomReconnectJitterMs);
            } catch(InterruptedException e) {
                logger.debug("Thread interrupted", e);
            }
        }
    }
}
