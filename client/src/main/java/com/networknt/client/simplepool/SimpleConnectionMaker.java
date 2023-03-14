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
 */
package com.networknt.client.simplepool;

import java.net.URI;
import java.util.Set;

/***
 * A factory that creates raw connections and wraps them in SimpleConnection objects.
 * SimpleConnectionMakers are used by SimpleConnectionHolders to create connections.
 *
 */
public interface SimpleConnectionMaker {
    public SimpleConnection makeConnection(long createConnectionTimeout, boolean isHttp2, final URI uri, final Set<SimpleConnection> allCreatedConnections);
    public SimpleConnection reuseConnection(long createConnectionTimeout, SimpleConnection connection) throws RuntimeException;
}
