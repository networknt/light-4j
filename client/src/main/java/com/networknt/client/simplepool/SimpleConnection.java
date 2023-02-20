package com.networknt.client.simplepool;

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
     * Returns the client side address of the conection
     * @return the client side address of the connection
     */
    public String getLocalAddress();

    /**
     * Safely closes the connection
     */
    public void safeClose();
}
