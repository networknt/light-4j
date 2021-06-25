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

package io.undertow.server.handlers.proxy;

import com.networknt.client.ClientConfig;
import com.networknt.client.ServerExchangeCarrier;
import com.networknt.cluster.Cluster;
import com.networknt.config.ConfigException;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.httpstring.HttpStringConstants;
import com.networknt.router.HostWhitelist;
import com.networknt.service.SingletonServiceFactory;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import io.undertow.client.UndertowClient;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.undertow.util.AttachmentList;
import io.undertow.util.CopyOnWriteMap;
import io.undertow.util.HeaderMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;
import org.xnio.ssl.XnioSsl;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.undertow.server.handlers.proxy.ProxyConnectionPool.AvailabilityType.*;

/**
 * This is a proxy client that supports multiple downstream services.
 *
 * @author Steve Hu
 */
public class LoadBalancingRouterProxyClient implements ProxyClient {
    private static Logger logger = LoggerFactory.getLogger(LoadBalancingRouterProxyClient.class);
    private static final AttachmentKey<AttachmentList<Host>> ATTEMPTED_HOSTS = AttachmentKey.createList(Host.class);
    private static Cluster cluster = SingletonServiceFactory.getBean(Cluster.class);
    private static final HostWhitelist HOST_WHITELIST = new HostWhitelist();

    /**
     * Time in seconds between retries for problem servers
     */
    private volatile int problemServerRetry = 10;

    /**
     * The number of connections to create per thread
     */
    private volatile int connectionsPerThread = 10;
    private volatile int maxQueueSize = 0;
    private volatile int softMaxConnectionsPerThread = 5;
    private volatile int ttl = -1;

    /**
     * The service hosts list map
     */
    private volatile Map<String, Host[]> hosts = new CopyOnWriteMap<>();

    private final HostSelector hostSelector;
    private final UndertowClient client;

    /**
     * These needs to be come from configuration
     */
    private XnioSsl ssl;
    private OptionMap options;
    private InetSocketAddress bindAddress;

    private static final ProxyTarget PROXY_TARGET = new ProxyTarget() {
    };

    public LoadBalancingRouterProxyClient() {
        this(UndertowClient.getInstance());
    }

    public LoadBalancingRouterProxyClient(UndertowClient client) {
        this(client, null);
    }

    public LoadBalancingRouterProxyClient(UndertowClient client, HostSelector hostSelector) {
        this.client = client;
        if (hostSelector == null) {
            this.hostSelector = new RoundRobinHostSelector();
        } else {
            this.hostSelector = hostSelector;
        }
    }

    public LoadBalancingRouterProxyClient setSsl(final XnioSsl ssl) {
        this.ssl = ssl;
        return this;
    }

    public LoadBalancingRouterProxyClient setOptionMap(final OptionMap options) {
        this.options = options;
        return this;
    }

    public LoadBalancingRouterProxyClient setProblemServerRetry(int problemServerRetry) {
        this.problemServerRetry = problemServerRetry;
        return this;
    }

    public int getProblemServerRetry() {
        return problemServerRetry;
    }

    public int getConnectionsPerThread() {
        return connectionsPerThread;
    }

    public LoadBalancingRouterProxyClient setConnectionsPerThread(int connectionsPerThread) {
        this.connectionsPerThread = connectionsPerThread;
        return this;
    }

    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    public LoadBalancingRouterProxyClient setMaxQueueSize(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
        return this;
    }

    public LoadBalancingRouterProxyClient setTtl(int ttl) {
        this.ttl = ttl;
        return this;
    }

    public LoadBalancingRouterProxyClient setSoftMaxConnectionsPerThread(int softMaxConnectionsPerThread) {
        this.softMaxConnectionsPerThread = softMaxConnectionsPerThread;
        return this;
    }

    public synchronized void addHosts(final String serviceId, final String envTag) {
        String key = envTag == null ? serviceId : serviceId + "|" + envTag;
        List<URI> uris = cluster.services(ssl == null ? "http" : "https", serviceId, envTag);
        hosts.remove(key);
        Host[] newHosts = new Host[uris.size()];
        for (int i = 0; i < uris.size(); i++) {
            Host h = new Host(serviceId, bindAddress, uris.get(i), ssl, options);
            newHosts[i] = h;
        }
        hosts.put(key, newHosts);
    }

    @Override
    public ProxyTarget findTarget(HttpServerExchange exchange) {
        return PROXY_TARGET;
    }

    @Override
    public void getConnection(ProxyTarget target, HttpServerExchange exchange, final ProxyCallback<ProxyConnection> callback, long timeout, TimeUnit timeUnit) {
        try {
            Host host = selectHost(exchange);
            if (host == null) {
                // give it second chance for service discovery again when problem occurs.
                host = selectHost(exchange);
            }
            if (host == null) {
                callback.couldNotResolveBackend(exchange);
            } else {
                exchange.addToAttachmentList(ATTEMPTED_HOSTS, host);
                host.connectionPool.connect(target, exchange, callback, timeout, timeUnit, false);
            }
        } catch (Exception ex) {
            logger.error("Failed to get connection", ex);
            exchange.setReasonPhrase(ex.getMessage());
            callback.failed(exchange);
        }
    }

    protected Host selectHost(HttpServerExchange exchange) {
        // get serviceId, env tag and hash key from header.
        HeaderMap headers = exchange.getRequestHeaders();
        String serviceId = headers.getFirst(HttpStringConstants.SERVICE_ID);
        String serviceUrl = headers.getFirst(HttpStringConstants.SERVICE_URL);
        // remove the header here in case the downstream service is another light-router instance.
        if(serviceUrl != null) headers.remove(HttpStringConstants.SERVICE_URL);
        String envTag = headers.getFirst(HttpStringConstants.ENV_TAG);
        String key = envTag == null ?  (serviceUrl != null ? serviceUrl : serviceId) :  (serviceUrl != null ? serviceUrl : serviceId) + "|" + envTag;
        AttachmentList<Host> attempted = exchange.getAttachment(ATTEMPTED_HOSTS);
        Host[] hostArray = this.hosts.get(key);
        if (hostArray == null || hostArray.length == 0) {
            // this must be the first this service is called since the router is started. discover here.
            if (serviceUrl != null) {
                try {
                    URI uri = new URI(serviceUrl);
                    if (HOST_WHITELIST != null) {
                        if (HOST_WHITELIST.isHostAllowed(uri)) {
                            this.hosts.put(key, new Host[] {new Host(serviceId, bindAddress, uri, ssl, options) });
                        } else {
                            throw new RuntimeException(String.format("Route to %s is not allowed in the host whitelist", serviceUrl));
                        }

                    } else {
                        throw new ConfigException(
                                String.format("Host Whitelist must be enabled to support route based on %s in Http header",
                                        HttpStringConstants.SERVICE_URL));
                    }
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            } else {
                addHosts(serviceId, envTag);
            }
            hostArray = this.hosts.get(key);
        }

        int host = hostSelector.selectHost(hostArray);

        final int startHost = host; //if the all hosts have problems we come back to this one
        Host full = null;
        Host problem = null;
        do {
            Host selected = hostArray[host];
            if (attempted == null || !attempted.contains(selected)) {
                ProxyConnectionPool.AvailabilityType available = selected.connectionPool.available();
                if (available == AVAILABLE) {
                    // inject the jaeger tracer.
                    injectTracer(exchange, selected);
                    return selected;
                } else if (available == FULL && full == null) {
                    full = selected;
                } else if ((available == PROBLEM || available == FULL_QUEUE) && problem == null) {
                    problem = selected;
                }
            }
            host = (host + 1) % hostArray.length;
        } while (host != startHost);
        if (full != null) {
            // inject the jaeger tracer.
            injectTracer(exchange, full);
            return full;
        }
        if (problem != null) {
            addHosts(serviceId, envTag);
        }
        //no available hosts
        return null;
    }

    private void injectTracer(HttpServerExchange exchange, Host host) {
        if(ClientConfig.get().isInjectOpenTracing()) {
            Tracer tracer = exchange.getAttachment(AttachmentConstants.EXCHANGE_TRACER);
            Span rootSpan = exchange.getAttachment(AttachmentConstants.ROOT_SPAN);
            if(tracer != null) {
                tracer.activateSpan(rootSpan);
                Tags.SPAN_KIND.set(tracer.activeSpan(), Tags.SPAN_KIND_CLIENT);
                Tags.HTTP_METHOD.set(tracer.activeSpan(), exchange.getRequestMethod().toString());
                Tags.HTTP_URL.set(tracer.activeSpan(), exchange.getRequestURI());
                Tags.PEER_PORT.set(tracer.activeSpan(), host.uri.getPort());
                Tags.PEER_HOSTNAME.set(tracer.activeSpan(), host.uri.getHost());
                tracer.inject(tracer.activeSpan().context(), Format.Builtin.HTTP_HEADERS, new ServerExchangeCarrier(exchange));
            }
        }
    }

    /**
     * Should only be used for tests
     * <p>
     * DO NOT CALL THIS METHOD WHEN REQUESTS ARE IN PROGRESS
     * <p>
     * It is not thread safe so internal state can get messed up.
     */
    public void closeCurrentConnections() {
        for (Map.Entry<String, Host[]> entry : hosts.entrySet()) {
            for (Host host : entry.getValue()) {
                host.closeCurrentConnections();
            }
        }
    }

    public final class Host extends ConnectionPoolErrorHandler.SimpleConnectionPoolErrorHandler implements ConnectionPoolManager {
        final ProxyConnectionPool connectionPool;
        final String serviceId;
        final URI uri;
        final XnioSsl ssl;

        private Host(String serviceId, InetSocketAddress bindAddress, URI uri, XnioSsl ssl, OptionMap options) {
            this.connectionPool = new ProxyConnectionPool(this, bindAddress, uri, ssl, client, options);
            this.serviceId = serviceId;
            this.uri = uri;
            this.ssl = ssl;
        }

        @Override
        public int getProblemServerRetry() {
            return problemServerRetry;
        }

        @Override
        public int getMaxConnections() {
            return connectionsPerThread;
        }

        @Override
        public int getMaxCachedConnections() {
            return connectionsPerThread;
        }

        @Override
        public int getSMaxConnections() {
            return softMaxConnectionsPerThread;
        }

        @Override
        public long getTtl() {
            return ttl;
        }

        @Override
        public int getMaxQueueSize() {
            return maxQueueSize;
        }

        public URI getUri() {
            return uri;
        }

        void closeCurrentConnections() {
            connectionPool.closeCurrentConnections();
        }
    }

    public interface HostSelector {

        int selectHost(Host[] availableHosts);
    }

    static class RoundRobinHostSelector implements HostSelector {

        private final AtomicInteger currentHost = new AtomicInteger(0);

        @Override
        public int selectHost(Host[] availableHosts) {
            return currentHost.incrementAndGet() % availableHosts.length;
        }
    }

}
