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
package com.networknt.client.simplepool.undertow;

import com.networknt.client.simplepool.SimpleConnection;
import io.undertow.client.ClientConnection;
import org.xnio.IoUtils;

public class SimpleClientConnection implements SimpleConnection {
    private ClientConnection connection;

    public SimpleClientConnection(ClientConnection connection) {
        this.connection = connection;
    }

    @Override
    public boolean isOpen() {
        return connection.isOpen();
    }

    @Override
    public Object getRawConnection() {
        return connection;
    }

    @Override
    public boolean isMultiplexingSupported() {
        return connection.isMultiplexingSupported();
    }

    @Override
    public String getLocalAddress() {
        return connection.getLocalAddress().toString();
    }

    @Override
    public void safeClose() {
        if(connection.isOpen())
            IoUtils.safeClose(connection);
    }
}
