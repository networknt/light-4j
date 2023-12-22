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
package com.networknt.client.simplepool.mock.mockexample;

import com.networknt.client.simplepool.SimpleConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

public class MockTimeoutLeakedConnection implements SimpleConnection {

    private volatile boolean closed = false;
    private boolean isHttp2 = true;
    private String MOCK_PORT = Integer.toString(ThreadLocalRandom.current().nextInt((int) (Math.pow(2, 15) - 1.0), (int) (Math.pow(2, 16) - 1.0)));
    private String MOCK_ADDRESS = "MOCK_HOST_IP:" + MOCK_PORT;
    private static final Logger logger = LoggerFactory.getLogger(MockTimeoutLeakedConnection.class);

    /***
     * This mock connection simulates a multiplexable connection that has a 20% chance of taking longer than 5s
     * to be created
     */

    public MockTimeoutLeakedConnection(boolean isHttp2) {
        this.isHttp2 = isHttp2;
        int time = randomCreationDelay();
        if(time > 0)
            logger.debug("OVERTIMEOUT DELAY: Connection {} created after a delay of {}s", MOCK_PORT, time/1000);
    }

    private int randomCreationDelay() throws RuntimeException {
        int time = 0;
        if(ThreadLocalRandom.current().nextInt(5) == 0) {
            try {
                time = 10*1000 + ThreadLocalRandom.current().nextInt(5) * 5000;
                Thread.sleep(time);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return time;
    }

    @Override
    public boolean isOpen() {
        return !closed;
    }

    @Override
    public Object getRawConnection() {
        throw new RuntimeException("Mock connection has no raw connection");
    }

    @Override
    public boolean isMultiplexingSupported() {
        return isHttp2;
    }

    @Override
    public String getLocalAddress() {
        return MOCK_ADDRESS;
    }

    @Override
    public void safeClose() {
        closed = true;
    }
}
