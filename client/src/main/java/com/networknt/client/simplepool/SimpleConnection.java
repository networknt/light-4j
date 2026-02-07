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

import com.networknt.client.simplepool.SimpleConnectionHolder;

/***
 * SimpleConnection is an interface that contains all the required functions and properties of
 * a connection that are needed by the SimpleConnectionHolder, SimpleURIConnectionPool, and
 * SimpleConnectionPool classes.
 *
 * Concrete HTTP network connections (like Undertow's ClientConnection class) should be wrapped in
 * classes that implement the SimpleConnection interface.
 *
 */
public interface SimpleConnection {
    /**
     * Tells whether or not the connection is open
     * @return returns true iff the raw connection is open
     */
    public boolean isOpen();

    /**
     * Returns the raw connection object. This must always be non-null
     * @return returns the raw connection object
     */
    public Object getRawConnection();

    /**
     * Tells whether the connection supports HTTP/2 connection multiplexing
     * @return returns true iff the connection supports HTTP/2 connection multiplexing
     */
    public boolean isMultiplexingSupported();

    /**
     * Returns the client side address of the connection
     * @return the client side address of the connection
     */
    public String getLocalAddress();

    /**
     * Safely closes the connection
     */
    public void safeClose();
}
