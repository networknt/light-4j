package com.networknt.whitelist;

import org.xnio.Bits;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class WhitelistConfig {
    /**
     * Standard IP address
     */
    private static final Pattern IP4_EXACT = Pattern.compile("(?:\\d{1,3}\\.){3}\\d{1,3}");

    /**
     * Standard IP address, with some octets replaced by a '*'
     */
    private static final Pattern IP4_WILDCARD = Pattern.compile("(?:(?:\\d{1,3}|\\*)\\.){3}(?:\\d{1,3}|\\*)");

    /**
     * IPv4 address with subnet specified via slash notation
     */
    private static final Pattern IP4_SLASH = Pattern.compile("(?:\\d{1,3}\\.){3}\\d{1,3}\\/\\d\\d?");

    /**
     * Standard full IPv6 address
     */
    private static final Pattern IP6_EXACT = Pattern.compile("(?:[a-zA-Z0-9]{1,4}:){7}[a-zA-Z0-9]{1,4}");

    /**
     * Standard full IPv6 address, with some parts replaced by a '*'
     */
    private static final Pattern IP6_WILDCARD = Pattern.compile("(?:(?:[a-zA-Z0-9]{1,4}|\\*):){7}(?:[a-zA-Z0-9]{1,4}|\\*)");

    /**
     * Standard full IPv6 address with subnet specified via slash notation
     */
    private static final Pattern IP6_SLASH = Pattern.compile("(?:[a-zA-Z0-9]{1,4}:){7}[a-zA-Z0-9]{1,4}\\/\\d{1,3}");

    boolean enabled;
    boolean defaultAllow;
    Map<String, IpAcl> endpointAcl = new HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDefaultAllow() {
        return defaultAllow;
    }

    public void setDefaultAllow(boolean defaultAllow) {
        this.defaultAllow = defaultAllow;
    }

    public void setPaths(Map<String, List<String>> paths) {
        for(Map.Entry<String, List<String>> entry: paths.entrySet()) {
            for(String peer: entry.getValue()) {
                addRule(entry.getKey(), peer, !this.defaultAllow);
            }
        }
    }

    private void addRule(final String endpoint, final String peer, final boolean deny) {
        if (IP4_EXACT.matcher(peer).matches()) {
            addIpV4ExactMatch(endpoint, peer, deny);
        } else if (IP4_WILDCARD.matcher(peer).matches()) {
            addIpV4WildcardMatch(endpoint, peer, deny);
        } else if (IP4_SLASH.matcher(peer).matches()) {
            addIpV4SlashPrefix(endpoint, peer, deny);
        } else if (IP6_EXACT.matcher(peer).matches()) {
            addIpV6ExactMatch(endpoint, peer, deny);
        } else if (IP6_WILDCARD.matcher(peer).matches()) {
            addIpV6WildcardMatch(endpoint, peer, deny);
        } else if (IP6_SLASH.matcher(peer).matches()) {
            addIpV6SlashPrefix(endpoint, peer, deny);
        } else {
            throw new RuntimeException("InvalidIpPattern:" + peer);
        }
    }

    private void addIpV6SlashPrefix(final String endpoint, final String peer, final boolean deny) {
        String[] components = peer.split("\\/");
        String[] parts = components[0].split("\\:");
        int maskLen = Integer.parseInt(components[1]);
        assert parts.length == 8;


        byte[] pattern = new byte[16];
        byte[] mask = new byte[16];

        for (int i = 0; i < 8; ++i) {
            int val = Integer.parseInt(parts[i], 16);
            pattern[i * 2] = (byte) (val >> 8);
            pattern[i * 2 + 1] = (byte) (val & 0xFF);
        }
        for (int i = 0; i < 16; ++i) {
            if (maskLen > 8) {
                mask[i] = (byte) (0xFF);
                maskLen -= 8;
            } else if (maskLen != 0) {
                mask[i] = (byte) (Bits.intBitMask(8 - maskLen, 7) & 0xFF);
                maskLen = 0;
            } else {
                break;
            }
        }
        IpAcl ipAcl = endpointAcl.get(endpoint);
        if(ipAcl == null) {
            ipAcl = new IpAcl();
            endpointAcl.put(endpoint, ipAcl);
        }
        ipAcl.getIpv6acl().add(new WhitelistHandler.PrefixIpV6PeerMatch(deny, peer, mask, pattern));
    }

    private void addIpV4SlashPrefix(final String endpoint, final String peer, final boolean deny) {
        String[] components = peer.split("\\/");
        String[] parts = components[0].split("\\.");
        int maskLen = Integer.parseInt(components[1]);
        final int mask = Bits.intBitMask(32 - maskLen, 31);
        int prefix = 0;
        for (int i = 0; i < 4; ++i) {
            prefix <<= 8;
            String part = parts[i];
            int no = Integer.parseInt(part);
            prefix |= no;
        }
        IpAcl ipAcl = endpointAcl.get(endpoint);
        if(ipAcl == null) {
            ipAcl = new IpAcl();
            endpointAcl.put(endpoint, ipAcl);
        }
        ipAcl.getIpv4acl().add(new WhitelistHandler.PrefixIpV4PeerMatch(deny, peer, mask, prefix));
    }

    private void addIpV6WildcardMatch(final String endpoint, final String peer, final boolean deny) {
        byte[] pattern = new byte[16];
        byte[] mask = new byte[16];
        String[] parts = peer.split("\\:");
        assert parts.length == 8;
        for (int i = 0; i < 8; ++i) {
            if (!parts[i].equals("*")) {
                int val = Integer.parseInt(parts[i], 16);
                pattern[i * 2] = (byte) (val >> 8);
                pattern[i * 2 + 1] = (byte) (val & 0xFF);
                mask[i * 2] = (byte) (0xFF);
                mask[i * 2 + 1] = (byte) (0xFF);
            }
        }
        IpAcl ipAcl = endpointAcl.get(endpoint);
        if(ipAcl == null) {
            ipAcl = new IpAcl();
            endpointAcl.put(endpoint, ipAcl);
        }
        ipAcl.getIpv6acl().add(new WhitelistHandler.PrefixIpV6PeerMatch(deny, peer, mask, pattern));
    }

    private void addIpV4WildcardMatch(final String endpoint, final String peer, final boolean deny) {
        String[] parts = peer.split("\\.");
        int mask = 0;
        int prefix = 0;
        for (int i = 0; i < 4; ++i) {
            mask <<= 8;
            prefix <<= 8;
            String part = parts[i];
            if (!part.equals("*")) {
                int no = Integer.parseInt(part);
                mask |= 0xFF;
                prefix |= no;
            }
        }
        IpAcl ipAcl = endpointAcl.get(endpoint);
        if(ipAcl == null) {
            ipAcl = new IpAcl();
            endpointAcl.put(endpoint, ipAcl);
        }
        ipAcl.getIpv4acl().add(new WhitelistHandler.PrefixIpV4PeerMatch(deny, peer, mask, prefix));
    }

    private void addIpV6ExactMatch(final String endpoint, final String peer, final boolean deny) {
        byte[] bytes = new byte[16];
        String[] parts = peer.split("\\:");
        assert parts.length == 8;
        for (int i = 0; i < 8; ++i) {
            int val = Integer.parseInt(parts[i], 16);
            bytes[i * 2] = (byte) (val >> 8);
            bytes[i * 2 + 1] = (byte) (val & 0xFF);
        }
        IpAcl ipAcl = endpointAcl.get(endpoint);
        if(ipAcl == null) {
            ipAcl = new IpAcl();
            endpointAcl.put(endpoint, ipAcl);
        }
        ipAcl.getIpv6acl().add(new WhitelistHandler.ExactIpV6PeerMatch(deny, peer, bytes));
    }

    private void addIpV4ExactMatch(final String endpoint, final String peer, final boolean deny) {
        String[] parts = peer.split("\\.");
        byte[] bytes = {(byte) Integer.parseInt(parts[0]), (byte) Integer.parseInt(parts[1]), (byte) Integer.parseInt(parts[2]), (byte) Integer.parseInt(parts[3])};
        IpAcl ipAcl = endpointAcl.get(endpoint);
        if(ipAcl == null) {
            ipAcl = new IpAcl();
            endpointAcl.put(endpoint, ipAcl);
        }
        ipAcl.getIpv4acl().add(new WhitelistHandler.ExactIpV4PeerMatch(deny, peer, bytes));
    }

}
