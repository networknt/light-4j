/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
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
 */

package com.networknt.whitelist;

import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Map;

public class WhitelistHandler implements MiddlewareHandler {
    private static final Logger logger = LoggerFactory.getLogger(WhitelistHandler.class);
    private static final String INVALID_IP_FOR_PATH = "ERR10049";

    private String configName;

    private volatile HttpHandler next;

    public WhitelistHandler(String configName) {
        this.configName = configName;
        if(logger.isInfoEnabled()) logger.info("WhitelistHandler is constructed.");
    }

    public WhitelistHandler() {
        this(WhitelistConfig.CONFIG_NAME);
        if(logger.isInfoEnabled()) logger.info("WhitelistHandler is constructed.");
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if(logger.isDebugEnabled()) logger.debug("WhitelistHandler.handleRequest starts.");
        InetSocketAddress peer = exchange.getSourceAddress();
        String reqPath = exchange.getRequestPath();
        if(logger.isTraceEnabled()) logger.trace("IP = {} request path = {}", peer.toString(), reqPath);
        WhitelistConfig config = WhitelistConfig.load(configName);
        if (!isAllowed(peer.getAddress(), reqPath, config)) {
            if(logger.isTraceEnabled()) logger.trace("Invalid IP for the path");
            setExchangeStatus(exchange, INVALID_IP_FOR_PATH, peer.toString(), reqPath);
            exchange.endExchange();
            return;
        }
        if(logger.isDebugEnabled()) logger.debug("WhitelistHandler.handleRequest ends.");
        Handler.next(exchange, next);
    }

    @Override
    public HttpHandler getNext() {
        return next;
    }

    @Override
    public MiddlewareHandler setNext(final HttpHandler next) {
        Handlers.handlerNotNull(next);
        this.next = next;
        return this;
    }

    @Override
    public boolean isEnabled() {
        return WhitelistConfig.load(configName).isEnabled();
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(configName, WhitelistHandler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(configName), null);
    }

    IpAcl findIpAcl(String reqPath, WhitelistConfig config) {
        for(Map.Entry<String, IpAcl> entry: config.getPrefixAcl().entrySet()) {
            if(reqPath.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
    boolean isAllowed(InetAddress address, String reqPath, WhitelistConfig config) {
        boolean isWhitelisted = false;
        if(address instanceof Inet4Address) {
            IpAcl ipAcl = findIpAcl(reqPath, config);
            if(ipAcl != null) {
                if(logger.isTraceEnabled()) logger.trace("IPv4 address and found a prefix entry for the request path");
                for (PeerMatch rule : ipAcl.getIpv4acl()) {
                    if (rule.matches(address)) {
                        if(logger.isTraceEnabled()) logger.trace("Found matched rule for address and rule isAllow {}", !rule.isDeny());
                        return !rule.isDeny();
                    }
                }
                // the path is defined but the IP is not in the list. Will allow if defaultAllow is false and will reject is defaultAllow is true
                return !config.defaultAllow;
            }
        } else if(address instanceof Inet6Address) {
            IpAcl ipAcl = findIpAcl(reqPath, config);
            if(ipAcl != null) {
                if(logger.isTraceEnabled()) logger.trace("IPv6 address {} and found a prefix entry for the request path {}", address, reqPath);
                for (PeerMatch rule : ipAcl.getIpv6acl()) {
                    if (rule.matches(address)) {
                        if(logger.isTraceEnabled()) logger.trace("Found matched rule for address and rule isAllow {}", !rule.isDeny());
                        return !rule.isDeny();
                    }
                }
                // the path is defined but the IP is not in the list. Will allow if defaultAllow is false and will reject is defaultAllow is true
                return !config.defaultAllow;
            }
        }
        if(logger.isTraceEnabled()) logger.trace("No matched path is found. isWhitelist is {} and defaultAllow is {}", isWhitelisted, config.defaultAllow);
        return config.defaultAllow;
    }



    abstract static class PeerMatch {

        private final boolean deny;
        private final String pattern;

        protected PeerMatch(final boolean deny, final String pattern) {
            this.deny = deny;
            this.pattern = pattern;
        }

        abstract boolean matches(final InetAddress address);

        boolean isDeny() {
            return deny;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                    "deny=" + deny +
                    ", pattern='" + pattern + '\'' +
                    '}';
        }
    }

    static class ExactIpV4PeerMatch extends WhitelistHandler.PeerMatch {

        private final byte[] address;

        protected ExactIpV4PeerMatch(final boolean deny, final String pattern, final byte[] address) {
            super(deny, pattern);
            this.address = address;
        }

        @Override
        boolean matches(final InetAddress address) {
            return Arrays.equals(address.getAddress(), this.address);
        }
    }

    static class ExactIpV6PeerMatch extends WhitelistHandler.PeerMatch {

        private final byte[] address;

        protected ExactIpV6PeerMatch(final boolean deny, final String pattern, final byte[] address) {
            super(deny, pattern);
            this.address = address;
        }

        @Override
        boolean matches(final InetAddress address) {
            return Arrays.equals(address.getAddress(), this.address);
        }
    }

    static class PrefixIpV4PeerMatch extends WhitelistHandler.PeerMatch {

        private final int mask;
        private final int prefix;

        protected PrefixIpV4PeerMatch(final boolean deny, final String pattern, final int mask, final int prefix) {
            super(deny, pattern);
            this.mask = mask;
            this.prefix = prefix;
        }

        @Override
        boolean matches(final InetAddress address) {
            byte[] bytes = address.getAddress();
            if (bytes == null) {
                return false;
            }
            int addressInt = ((bytes[0] & 0xFF) << 24) | ((bytes[1] & 0xFF) << 16) | ((bytes[2] & 0xFF) << 8) | (bytes[3] & 0xFF);
            return (addressInt & mask) == prefix;
        }
    }

    static class PrefixIpV6PeerMatch extends WhitelistHandler.PeerMatch {

        private final byte[] mask;
        private final byte[] prefix;

        protected PrefixIpV6PeerMatch(final boolean deny, final String pattern, final byte[] mask, final byte[] prefix) {
            super(deny, pattern);
            this.mask = mask;
            this.prefix = prefix;
            assert mask.length == prefix.length;
        }

        @Override
        boolean matches(final InetAddress address) {
            byte[] bytes = address.getAddress();
            if (bytes == null) {
                return false;
            }
            if (bytes.length != mask.length) {
                return false;
            }
            for (int i = 0; i < mask.length; ++i) {
                if ((bytes[i] & mask[i]) != prefix[i]) {
                    return false;
                }
            }
            return true;
        }
    }
}
