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
package com.networknt.client.simplepool.mockexample;

import com.networknt.client.simplepool.TestRunner;

public class TestPoolSizeOverflow
{
    public static void main(String[] args) {
        new TestRunner()
            // set connection properties
            .setConnectionPoolSize(7)
            .setSimpleConnectionClass(MockKeepAliveConnection.class)
            .setCreateConnectionTimeout(5)
            .setConnectionExpireTime(5)
            .setHttp2(false)

            // configure borrower-thread properties
            .setNumBorrowerThreads(8)
            .setBorrowerThreadStartJitter(0)
            .setBorrowTimeLength(2)
            .setBorrowTimeLengthJitter(8)
            .setWaitTimeBeforeReborrow(1)
            .setWaitTimeBeforeReborrowJitter(1)

            .setTestLength(10*60)
            .executeTest();
    }
}
