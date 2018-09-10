package com.networknt.consul;

public class ConsulConfig {
    String consulUrl;
    int maxReqPerConn;
    String deregisterAfter;
    String checkInterval;
    boolean tcpCheck;
    boolean httpCheck;
    boolean ttlCheck;

    public String getConsulUrl() {
        return consulUrl;
    }

    public void setConsulUrl(String consulUrl) {
        this.consulUrl = consulUrl;
    }

    public int getMaxReqPerConn() { return maxReqPerConn; }

    public void setMaxReqPerConn(int maxReqPerConn) { this.maxReqPerConn = maxReqPerConn; }

    public String getDeregisterAfter() {
        return deregisterAfter;
    }

    public void setDeregisterAfter(String deregisterAfter) {
        this.deregisterAfter = deregisterAfter;
    }

    public String getCheckInterval() {
        return checkInterval;
    }

    public void setCheckInterval(String checkInterval) {
        this.checkInterval = checkInterval;
    }

    public boolean isTcpCheck() {
        return tcpCheck;
    }

    public void setTcpCheck(boolean tcpCheck) {
        this.tcpCheck = tcpCheck;
    }

    public boolean isHttpCheck() {
        return httpCheck;
    }

    public void setHttpCheck(boolean httpCheck) {
        this.httpCheck = httpCheck;
    }

    public boolean isTtlCheck() {
        return ttlCheck;
    }

    public void setTtlCheck(boolean ttlCheck) {
        this.ttlCheck = ttlCheck;
    }
}
