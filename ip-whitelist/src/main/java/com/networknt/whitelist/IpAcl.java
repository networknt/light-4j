package com.networknt.whitelist;

import java.util.ArrayList;
import java.util.List;

public class IpAcl {
    private List<WhitelistHandler.PeerMatch> ipv6acl = new ArrayList<>();
    private List<WhitelistHandler.PeerMatch> ipv4acl = new ArrayList<>();

    public List<WhitelistHandler.PeerMatch> getIpv6acl() {
        return ipv6acl;
    }

    public void setIpv6acl(List<WhitelistHandler.PeerMatch> ipv6acl) {
        this.ipv6acl = ipv6acl;
    }

    public List<WhitelistHandler.PeerMatch> getIpv4acl() {
        return ipv4acl;
    }

    public void setIpv4acl(List<WhitelistHandler.PeerMatch> ipv4acl) {
        this.ipv4acl = ipv4acl;
    }
}
