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

public class WhitelistHandler implements MiddlewareHandler {
    private static final Logger logger = LoggerFactory.getLogger(WhitelistHandler.class);
    private static final String CONFIG_NAME = "whitelist";
    private static final String INVALID_IP_FOR_PATH = "ERR10049";

    public static WhitelistConfig config =
            (WhitelistConfig)Config.getInstance().getJsonObjectConfig(CONFIG_NAME, WhitelistConfig.class);

    private volatile HttpHandler next;

    public WhitelistHandler() {
        if(logger.isInfoEnabled()) logger.info("WhitelistHandler is constructed.");
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        InetSocketAddress peer = exchange.getSourceAddress();
        String endpoint = exchange.getRelativePath() + "@" + exchange.getRequestMethod().toString().toLowerCase();
        if (!isAllowed(peer.getAddress(), endpoint)) {
            setExchangeStatus(exchange, INVALID_IP_FOR_PATH, peer.toString(), endpoint);
            return;
        }
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
        return config.isEnabled();
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(WhitelistHandler.class.getName(), Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME), null);
    }

    boolean isAllowed(InetAddress address, String endpoint) {
        boolean isWhitelisted = false;
        if(address instanceof Inet4Address) {
            IpAcl ipAcl = config.endpointAcl.get(endpoint);
            if(ipAcl != null) {
                for (PeerMatch rule : ipAcl.getIpv4acl()) {
                    if (rule.matches(address)) {
                        return !rule.isDeny();
                    }
                }
                isWhitelisted = true;
            }
        } else if(address instanceof Inet6Address) {
            IpAcl ipAcl = config.endpointAcl.get(endpoint);
            if(ipAcl != null) {
                for (PeerMatch rule : ipAcl.getIpv6acl()) {
                    if (rule.matches(address)) {
                        return !rule.isDeny();
                    }
                }
                isWhitelisted = true;
            }
        }
        return !isWhitelisted && config.defaultAllow;
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
