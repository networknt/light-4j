package com.networknt.resource;

public class VirtualHost {
    String path;
    String domain;
    String base;
    int transferMinSize;
    boolean directoryListingEnabled;

    public VirtualHost() {
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public int getTransferMinSize() {
        return transferMinSize;
    }

    public void setTransferMinSize(int transferMinSize) {
        this.transferMinSize = transferMinSize;
    }

    public boolean isDirectoryListingEnabled() {
        return directoryListingEnabled;
    }

    public void setDirectoryListingEnabled(boolean directoryListingEnabled) {
        this.directoryListingEnabled = directoryListingEnabled;
    }
}
