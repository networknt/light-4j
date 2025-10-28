/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */


package com.networknt.whitelist;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.JsonMapper;
import com.networknt.config.schema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.Bits;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@ConfigSchema(
        configKey = "whitelist",
        configName = "whitelist",
        configDescription = "IP Whitelist configuration",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML}
)
public class WhitelistConfig {
    public static final Logger logger = LoggerFactory.getLogger(WhitelistConfig.class);

    public static final String CONFIG_NAME = "whitelist";
    public static final String ENABLED = "enabled";
    public static final String DEFAULT_ALLOW = "defaultAllow";
    public static final String PATHS = "paths";

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

    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            externalized = true,
            defaultValue = "true",
            description = """
                    Indicate if this handler is enabled or not. It is normally used for the third party integration
                    so that only approved IPs can connect to the light-gateway or http-sidecar at certain endpoints.
                    The default value is true, and it will take effect as long as this handler is in the chain.
                    """
    )
    boolean enabled;

    @BooleanField(
            configFieldName = DEFAULT_ALLOW,
            externalizedKeyName = DEFAULT_ALLOW,
            externalized = true,
            defaultValue = "true",
            description = """
                    Default allowed or denied if there is no rules defined for the path or the path is not defined.
                    If defaultAllow is true, all IP addresses defined under the matched path will be allowed and IP
                    addresses are not in the list will be denied. If the request path prefix is not defined, then all
                    requests will be allowed by default. Basically, only IPs not listed under a path will be denied.
                    If defaultAllow is false, all IP addresses defined under the matched path will be denied and IP
                    addresses are not in the list will be allowed. If the request path prefix is not defined, then all
                    requests will be denied by default. Basically, only IPs not listed under a path will be allowed.
                    """
    )
    boolean defaultAllow;

    @MapField(
            configFieldName = PATHS,
            externalizedKeyName = PATHS,
            externalized = true,
            description = """
                    List of path prefixes and their access rules. It supports IPv4 and IPv6 with Exact, Wildcard and
                    Slash format. The path prefix is defined as request path prefix only without differentiate method.
                    The following format is the YAML format suitable for externalized values.yml in local filesystem.
                     /health/com.networknt.petstore-1.0.0:
                       - 127.0.0.1
                       - 10.10.*.*
                       - 127.0.0.48/30
                     /prometheus:
                       - FE45:00:00:000:0:AAA:FFFF:0045
                       - FE45:00:00:000:0:AAA:FFFF:*
                       - FE45:00:00:000:0:AAA:FFFF:01F4/127
                     /data:
                       - 127.0.0.2
                       - 10.10.*.*
                       - 127.0.0.48/30
                    
                    The following format is the JSON format suitable for both local values.yml and config server.
                    paths: {"/health/com.networknt.petstore-1.0.0":["127.0.0.1","10.10.*.*","127.0.0.48/30"],"/prometheus":["FE45:00:00:000:0:AAA:FFFF:0045","FE45:00:00:000:0:AAA:FFFF:*","FE45:00:00:000:0:AAA:FFFF:01F4/127"],"/data":["127.0.0.2","10.10.*.*","127.0.0.48/30"]}
                    """,
            valueTypeOneOf = {List.class, String.class}
    )
    Map<String, IpAcl> prefixAcl = new HashMap<>();
    private Config config;
    private Map<String, Object> mappedConfig;

    private WhitelistConfig() {
        this(CONFIG_NAME);
    }

    /**
     * Please note that this constructor is only for testing to load different config files
     * to test different configurations.
     * @param configName String
     */
    private WhitelistConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setConfigMap();
    }
    public static WhitelistConfig load() {
        return new WhitelistConfig();
    }

    public static WhitelistConfig load(String configName) {
        return new WhitelistConfig(configName);
    }

    void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
        setConfigMap();
    }

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

    public Map<String, IpAcl> getPrefixAcl() {
        return prefixAcl;
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    private void setConfigData() {
        Object object = mappedConfig.get(ENABLED);
        if(object != null) enabled = Config.loadBooleanValue(ENABLED, object);
        object = mappedConfig.get(DEFAULT_ALLOW);
        if(object != null) defaultAllow = Config.loadBooleanValue(DEFAULT_ALLOW, object);
    }

    private void setConfigMap() {
        // paths white list mapping
        if (mappedConfig.get(PATHS) != null) {
            Object object = mappedConfig.get(PATHS);
            if(object != null) {
                Map<String, Object> paths;
                if(object instanceof String) {
                    String s = (String)object;
                    s = s.trim();
                    if(logger.isTraceEnabled()) logger.trace("paths = " + s);
                    if(s.startsWith("{")) {
                        // json format
                        try {
                            paths = JsonMapper.string2Map(s);
                        } catch (Exception e) {
                            throw new ConfigException("could not parse the paths with a map of string to string list.");
                        }
                    } else {
                        throw new ConfigException("paths must be a string start with { in json format.");
                    }
                } else if (object instanceof Map) {
                    paths = (Map)object;
                } else {
                    throw new ConfigException("paths must be string to string list map.");
                }
                setPaths(paths);
            }
        }
    }

    public void setPaths(Map<String, Object> paths) {
        for(Map.Entry<String, Object> entry: paths.entrySet()) {
            for(String peer: (List<String>)entry.getValue()) {
                addRule(entry.getKey(), peer, !this.defaultAllow);
            }
        }
    }

    private void addRule(final String pathPrefix, final String peer, final boolean deny) {
        if (IP4_EXACT.matcher(peer).matches()) {
            addIpV4ExactMatch(pathPrefix, peer, deny);
        } else if (IP4_WILDCARD.matcher(peer).matches()) {
            addIpV4WildcardMatch(pathPrefix, peer, deny);
        } else if (IP4_SLASH.matcher(peer).matches()) {
            addIpV4SlashPrefix(pathPrefix, peer, deny);
        } else if (IP6_EXACT.matcher(peer).matches()) {
            addIpV6ExactMatch(pathPrefix, peer, deny);
        } else if (IP6_WILDCARD.matcher(peer).matches()) {
            addIpV6WildcardMatch(pathPrefix, peer, deny);
        } else if (IP6_SLASH.matcher(peer).matches()) {
            addIpV6SlashPrefix(pathPrefix, peer, deny);
        } else {
            throw new RuntimeException("InvalidIpPattern:" + peer);
        }
    }

    private void addIpV6SlashPrefix(final String pathPrefix, final String peer, final boolean deny) {
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
        IpAcl ipAcl = prefixAcl.get(pathPrefix);
        if(ipAcl == null) {
            ipAcl = new IpAcl();
            prefixAcl.put(pathPrefix, ipAcl);
        }
        ipAcl.getIpv6acl().add(new WhitelistHandler.PrefixIpV6PeerMatch(deny, peer, mask, pattern));
    }

    private void addIpV4SlashPrefix(final String pathPrefix, final String peer, final boolean deny) {
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
        IpAcl ipAcl = prefixAcl.get(pathPrefix);
        if(ipAcl == null) {
            ipAcl = new IpAcl();
            prefixAcl.put(pathPrefix, ipAcl);
        }
        ipAcl.getIpv4acl().add(new WhitelistHandler.PrefixIpV4PeerMatch(deny, peer, mask, prefix));
    }

    private void addIpV6WildcardMatch(final String pathPrefix, final String peer, final boolean deny) {
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
        IpAcl ipAcl = prefixAcl.get(pathPrefix);
        if(ipAcl == null) {
            ipAcl = new IpAcl();
            prefixAcl.put(pathPrefix, ipAcl);
        }
        ipAcl.getIpv6acl().add(new WhitelistHandler.PrefixIpV6PeerMatch(deny, peer, mask, pattern));
    }

    private void addIpV4WildcardMatch(final String pathPrefix, final String peer, final boolean deny) {
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
        IpAcl ipAcl = prefixAcl.get(pathPrefix);
        if(ipAcl == null) {
            ipAcl = new IpAcl();
            prefixAcl.put(pathPrefix, ipAcl);
        }
        ipAcl.getIpv4acl().add(new WhitelistHandler.PrefixIpV4PeerMatch(deny, peer, mask, prefix));
    }

    private void addIpV6ExactMatch(final String pathPrefix, final String peer, final boolean deny) {
        byte[] bytes = new byte[16];
        String[] parts = peer.split("\\:");
        assert parts.length == 8;
        for (int i = 0; i < 8; ++i) {
            int val = Integer.parseInt(parts[i], 16);
            bytes[i * 2] = (byte) (val >> 8);
            bytes[i * 2 + 1] = (byte) (val & 0xFF);
        }
        IpAcl ipAcl = prefixAcl.get(pathPrefix);
        if(ipAcl == null) {
            ipAcl = new IpAcl();
            prefixAcl.put(pathPrefix, ipAcl);
        }
        ipAcl.getIpv6acl().add(new WhitelistHandler.ExactIpV6PeerMatch(deny, peer, bytes));
    }

    private void addIpV4ExactMatch(final String pathPrefix, final String peer, final boolean deny) {
        String[] parts = peer.split("\\.");
        byte[] bytes = {(byte) Integer.parseInt(parts[0]), (byte) Integer.parseInt(parts[1]), (byte) Integer.parseInt(parts[2]), (byte) Integer.parseInt(parts[3])};
        IpAcl ipAcl = prefixAcl.get(pathPrefix);
        if(ipAcl == null) {
            ipAcl = new IpAcl();
            prefixAcl.put(pathPrefix, ipAcl);
        }
        ipAcl.getIpv4acl().add(new WhitelistHandler.ExactIpV4PeerMatch(deny, peer, bytes));
    }

    @Override
    public String toString() {
        return "WhitelistConfig{" +
                "enabled=" + enabled +
                ", defaultAllow=" + defaultAllow +
                ", prefixAcl=" + prefixAcl +
                '}';
    }
}
