package com.networknt.client.http;

public interface SimpleConnection<Connection> {
    public boolean isOpen();
    public Connection getRawConnection();
    public boolean isMultiplexingSupported();
    public String getLocalAddress();
    public void safeClose();
}
