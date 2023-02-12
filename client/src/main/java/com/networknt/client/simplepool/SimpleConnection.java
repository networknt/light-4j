package com.networknt.client.simplepool;

public interface SimpleConnection {
    public boolean isOpen();
    public Object getRawConnection();
    public boolean isMultiplexingSupported();
    public String getLocalAddress();
    public void safeClose();
}
