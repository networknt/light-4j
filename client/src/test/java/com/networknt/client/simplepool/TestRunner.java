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
package com.networknt.client.simplepool;

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
    private Class simpleConnectionClass;
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

    /**
     * Test length in seconds. Default 120s
     * @param testLength long
     * @return TestRunner
     */
    public TestRunner setTestLength(long testLength) { this.testLength = testLength; return this; }
    /**
     * Number of borrowing threads. Default 2
     * @param numCallers int
     * @return TestRunner
     */
    public TestRunner setNumBorrowerThreads(int numCallers) { this.numCallers = numCallers; return this; }
    /**
     * Mock URI. Default https://mock-uri.com
     * @param uri URI
     * @return TestRunner
     */
    public TestRunner setUri(URI uri) { this.uri = uri; return this; }
    /**
     * Maximum number of connections allowed in the connection pool. Default 100
     * @param poolSize int
     * @return TestRunner
     */
    public TestRunner setConnectionPoolSize(int poolSize) { this.poolSize = poolSize; return this; }
    /**
     * Connection expiry time in seconds. Default 10s
     * @param expireTime long
     * @return TestRunner
     */
    public TestRunner setConnectionExpireTime(long expireTime) { this.expireTime = expireTime; return this; }
    /**
     * The SimpleConnection class used for connections -- must have a parameterless constructor.
     * Note: executeTest() will throw an exception if this is not set.
     * @param simpleConnectionClass Class
     * @return TestRunner
     */
    public TestRunner setSimpleConnectionClass(Class simpleConnectionClass) { this.simpleConnectionClass = simpleConnectionClass; return this; }
    /**
     * Connection creation timeout in seconds. Default is 5s
     * @param createConnectionTimeout long
     * @return TestRunner
     */
    public TestRunner setCreateConnectionTimeout(long createConnectionTimeout) { this.createConnectionTimeout = createConnectionTimeout; return this; }
    /**
     * Amount of time in seconds that borrower threads hold connections before restoring them. Default 3s
     * @param borrowTime long
     * @return TestRunner
     */
    public TestRunner setBorrowTimeLength(long borrowTime) { this.borrowTime = borrowTime; return this; }
    /**
     * Max random additional time in seconds that borrower threads hold connections before restoring them. Default 4s
     * @param borrowJitter long
     * @return TestRunner
     */
    public TestRunner setBorrowTimeLengthJitter(long borrowJitter) { this.borrowJitter = borrowJitter; return this; }
    /**
     * Amount of time in seconds that borrower threads waits after returning a connection to borrow again. Default 2s
     * @param reconnectTime long
     * @return TestRunner
     */
    public TestRunner setWaitTimeBeforeReborrow(long reconnectTime) { this.reconnectTime = reconnectTime; return this; }
    /**
     * Max random additional time in seconds that borrower threads waits after returning a connection to borrow again. Default 2s
     * @param reconnectTimeJitter long
     * @return TestRunner
     */
    public TestRunner setWaitTimeBeforeReborrowJitter(long reconnectTimeJitter) { this.reconnectTimeJitter = reconnectTimeJitter; return this; }
    /**
     * Max random startup delay in seconds for borrower threads. Default 3s
     * @param threadStartJitter int
     * @return TestRunner
     */
    public TestRunner setBorrowerThreadStartJitter(int threadStartJitter) { this.threadStartJitter = threadStartJitter; return this; }
    /**
     * Determines whether caller threads request HTTP/2 connections. HTTP/2 means multiple borrows per connection are allowed. Default true
     * @param http2 boolean
     * @return TestRunner
     */
    public TestRunner setHttp2(boolean http2) { isHttp2 = http2; return this; }

    public void executeTest() throws RuntimeException {
        if(simpleConnectionClass == null)
            throw new RuntimeException("A SimpleConnection class must be set using setSimpleConnectionClass()");

        try {
            // create connection maker
            connectionMaker = new TestConnectionMaker(simpleConnectionClass);
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
        private final CountDownLatch latch;
        private final AtomicBoolean stopped;
        private final SimpleURIConnectionPool pool;
        private final long createConnectionTimeout;
        private final long borrowTime;
        private final long borrowJitter;
        private final long reconnectTime;
        private final long reconnectTimeJitter;

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
                    connectionToken = pool.borrow(createConnectionTimeout);

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
            wait("{} Borrowing connection for {} seconds...", borrowTime, borrowJitter);
        }

        private void reborrowWaitTime(long reconnectTime, long reconnectTimeJitter) {
            wait("{} Waiting for {} seconds to borrow connection again...", borrowTime, borrowJitter);
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
