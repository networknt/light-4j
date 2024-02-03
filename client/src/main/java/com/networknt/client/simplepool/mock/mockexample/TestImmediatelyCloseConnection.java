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

import com.networknt.client.simplepool.mock.TestRunner;

public class TestImmediatelyCloseConnection
{
    public static void main(String[] args) {
        new TestRunner()
            // set connection properties
            .setConnectionPoolSize(100)
            .setSimpleConnectionClass(MockKeepAliveConnection.class)
            .setCreateConnectionTimeout(5)
            .setConnectionExpireTime(5)
            .setHttp2(true)

            // configure borrower-thread properties
            .setRestoreAndImmediatelyCloseFrequency(0.1)
            .setNumBorrowerThreads(4)
            .setBorrowerThreadStartJitter(0)
            .setBorrowTimeLength(5)
            .setBorrowTimeLengthJitter(5)
            .setWaitTimeBeforeReborrow(2)
            .setWaitTimeBeforeReborrowJitter(2)

            .setTestLength(10*60)
            .executeTest();
    }
}
