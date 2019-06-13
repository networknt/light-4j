/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.networknt.zookeeper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.networknt.registry.URLImpl;
import com.networknt.status.Status;
import com.networknt.zookeeper.client.ZooKeeperClient;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.apache.zookeeper.Watcher;

import com.networknt.utility.Constants;
import com.networknt.exception.FrameworkException;
import com.networknt.registry.support.command.CommandFailbackRegistry;
import com.networknt.registry.support.command.ServiceListener;
import com.networknt.registry.URL;
import com.networknt.utility.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ZooKeeper Registry implementation.
 *
 */
public class ZooKeeperRegistry extends CommandFailbackRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ZooKeeperRegistry.class);
    private static final String SUBSCRIBE_ZOOKEEPER_SERVICE_ERROR = "ERR10027";
    private static final String UNSUBSCRIBE_ZOOKEEPER_SERVICE_ERROR = "ERR10029";
    private static final String DISCOVER_ZOOKEEPER_SERVICE_ERROR = "ERR10031";
    private static final String REGISTER_ZOOKEEPER_ERROR = "ERR10033";
    private static final String UNREGISTER_ZOOKEEPER_ERROR = "ERR10034";

    private ZooKeeperClient client;
    private Set<URL> availableServices = new ConcurrentHashSet<URL>();
    private ConcurrentHashMap<URL, ConcurrentHashMap<ServiceListener, IZkChildListener>> serviceListeners = new ConcurrentHashMap<URL, ConcurrentHashMap<ServiceListener, IZkChildListener>>();
    private final ReentrantLock clientLock = new ReentrantLock();
    private final ReentrantLock serverLock = new ReentrantLock();
    
    public ZooKeeperRegistry(URL url, ZooKeeperClient client) {
        super(url);
        this.client = client;
        IZkStateListener zkStateListener = new IZkStateListener() {
            @Override
            public void handleStateChanged(Watcher.Event.KeeperState state) throws Exception {
                // do nothing
            }

            @Override
            public void handleNewSession() throws Exception {
                if(logger.isInfoEnabled()) logger.info("zkRegistry get new session notify.");
                reconnectService();
            }
        };
        client.subscribeStateChanges(zkStateListener);
    }

    public ConcurrentHashMap<URL, ConcurrentHashMap<ServiceListener, IZkChildListener>> getServiceListeners() {
        return serviceListeners;
    }

    @Override
    protected void subscribeService(final URL url, final ServiceListener serviceListener) {
        try {
            clientLock.lock();
            ConcurrentHashMap<ServiceListener, IZkChildListener> childChangeListeners = serviceListeners.get(url);
            if (childChangeListeners == null) {
                serviceListeners.putIfAbsent(url, new ConcurrentHashMap<ServiceListener, IZkChildListener>());
                childChangeListeners = serviceListeners.get(url);
            }
            IZkChildListener zkChildListener = childChangeListeners.get(serviceListener);
            if (zkChildListener == null) {
                childChangeListeners.putIfAbsent(serviceListener, new IZkChildListener() {
                    @Override
                    public void handleChildChange(String parentPath, List<String> currentChilds) {
                        serviceListener.notifyService(url, getUrl(), nodeChildsToUrls(parentPath, currentChilds));
                        if(logger.isInfoEnabled()) logger.info(String.format("[ZooKeeperRegistry] service list change: path=%s, currentChilds=%s", parentPath, currentChilds.toString()));
                    }
                });
                zkChildListener = childChangeListeners.get(serviceListener);
            }

            // prevent old node unregistered
            removeNode(url, ZkNodeType.CLIENT);
            createNode(url, ZkNodeType.CLIENT);

            String serverTypePath = ZkUtils.toNodeTypePath(url, ZkNodeType.AVAILABLE_SERVER);
            client.subscribeChildChanges(serverTypePath, zkChildListener);
            if(logger.isInfoEnabled()) logger.info(String.format("[ZooKeeperRegistry] subscribe service: path=%s, info=%s", ZkUtils.toNodePath(url, ZkNodeType.AVAILABLE_SERVER), url.toFullStr()));
        } catch (Throwable e) {
            throw new FrameworkException(new Status(SUBSCRIBE_ZOOKEEPER_SERVICE_ERROR, url, getUrl(), e.getMessage()), e);
        } finally {
            clientLock.unlock();
        }
    }

    @Override
    protected void unsubscribeService(URL url, ServiceListener serviceListener) {
        try {
            clientLock.lock();
            Map<ServiceListener, IZkChildListener> childChangeListeners = serviceListeners.get(url);
            if (childChangeListeners != null) {
                IZkChildListener zkChildListener = childChangeListeners.get(serviceListener);
                if (zkChildListener != null) {
                    client.unsubscribeChildChanges(ZkUtils.toNodeTypePath(url, ZkNodeType.CLIENT), zkChildListener);
                    childChangeListeners.remove(serviceListener);
                }
            }
        } catch (Throwable e) {
            throw new FrameworkException(new Status(UNSUBSCRIBE_ZOOKEEPER_SERVICE_ERROR, url, getUrl(), e.getMessage()), e);
        } finally {
            clientLock.unlock();
        }
    }

    @Override
    protected List<URL> discoverService(URL url) {
        try {
            String parentPath = ZkUtils.toNodeTypePath(url, ZkNodeType.AVAILABLE_SERVER);
            List<String> currentChilds = new ArrayList<String>();
            if (client.exists(parentPath)) {
                currentChilds = client.getChildren(parentPath);
            }
            return nodeChildsToUrls(parentPath, currentChilds);
        } catch (Throwable e) {
            throw new FrameworkException(new Status(DISCOVER_ZOOKEEPER_SERVICE_ERROR, url, getUrl(), e.getMessage()), e);
        }
    }

    @Override
    protected void doRegister(URL url) {
        try {
            serverLock.lock();
            removeNode(url, ZkNodeType.AVAILABLE_SERVER);
            removeNode(url, ZkNodeType.UNAVAILABLE_SERVER);
            createNode(url, ZkNodeType.UNAVAILABLE_SERVER);
        } catch (Throwable e) {
            throw new FrameworkException(new Status(REGISTER_ZOOKEEPER_ERROR, url, getUrl(), e.getMessage()), e);
        } finally {
            serverLock.unlock();
        }
    }

    @Override
    protected void doUnregister(URL url) {
        try {
            serverLock.lock();
            removeNode(url, ZkNodeType.AVAILABLE_SERVER);
            removeNode(url, ZkNodeType.UNAVAILABLE_SERVER);
        } catch (Throwable e) {
            throw new FrameworkException(new Status(UNREGISTER_ZOOKEEPER_ERROR, url, getUrl(), e.getMessage()), e);
        } finally {
            serverLock.unlock();
        }
    }

    @Override
    protected void doAvailable(URL url) {
        try{
            serverLock.lock();
            if (url == null) {
                availableServices.addAll(getRegisteredServiceUrls());
                for (URL u : getRegisteredServiceUrls()) {
                    removeNode(u, ZkNodeType.AVAILABLE_SERVER);
                    removeNode(u, ZkNodeType.UNAVAILABLE_SERVER);
                    createNode(u, ZkNodeType.AVAILABLE_SERVER);
                }
            } else {
                availableServices.add(url);
                removeNode(url, ZkNodeType.AVAILABLE_SERVER);
                removeNode(url, ZkNodeType.UNAVAILABLE_SERVER);
                createNode(url, ZkNodeType.AVAILABLE_SERVER);
            }
        } finally {
            serverLock.unlock();
        }
    }

    @Override
    protected void doUnavailable(URL url) {
        try{
            serverLock.lock();
            if (url == null) {
                availableServices.removeAll(getRegisteredServiceUrls());
                for (URL u : getRegisteredServiceUrls()) {
                    removeNode(u, ZkNodeType.AVAILABLE_SERVER);
                    removeNode(u, ZkNodeType.UNAVAILABLE_SERVER);
                    createNode(u, ZkNodeType.UNAVAILABLE_SERVER);
                }
            } else {
                availableServices.remove(url);
                removeNode(url, ZkNodeType.AVAILABLE_SERVER);
                removeNode(url, ZkNodeType.UNAVAILABLE_SERVER);
                createNode(url, ZkNodeType.UNAVAILABLE_SERVER);
            }
        } finally {
            serverLock.unlock();
        }
    }

    private List<URL> nodeChildsToUrls(String parentPath, List<String> currentChilds) {
        List<URL> urls = new ArrayList<URL>();
        if (currentChilds != null) {
            for (String node : currentChilds) {
                String nodePath = parentPath + Constants.PATH_SEPARATOR + node;
                String data = client.readData(nodePath, true);
                try {
                    URL url = URLImpl.valueOf(data);
                    urls.add(url);
                } catch (Exception e) {
                    if(logger.isInfoEnabled()) logger.warn(String.format("Found malformed urls from ZooKeeperRegistry, path=%s", nodePath), e);
                }
            }
        }
        return urls;
    }

    private void createNode(URL url, ZkNodeType nodeType) {
        String nodeTypePath = ZkUtils.toNodeTypePath(url, nodeType);
        if (!client.exists(nodeTypePath)) {
            client.createPersistent(nodeTypePath, true);
        }
        client.createEphemeral(ZkUtils.toNodePath(url, nodeType), url.toFullStr());
    }

    private void removeNode(URL url, ZkNodeType nodeType) {
        String nodePath = ZkUtils.toNodePath(url, nodeType);
        if (client.exists(nodePath)) {
            client.delete(nodePath);
        }
    }
    
    private void reconnectService() {
        Collection<URL> allRegisteredServices = getRegisteredServiceUrls();
        if (allRegisteredServices != null && !allRegisteredServices.isEmpty()) {
            try {
                serverLock.lock();
                for (URL url : getRegisteredServiceUrls()) {
                    doRegister(url);
                }
                if(logger.isInfoEnabled()) logger.info("[{}] reconnect: register services {}", registryClassName, allRegisteredServices);

                for (URL url : availableServices) {
                    if (!getRegisteredServiceUrls().contains(url)) {
                        if(logger.isWarnEnabled()) logger.warn("reconnect url not register. url:{}", url);
                        continue;
                    }
                    doAvailable(url);
                }
                if(logger.isInfoEnabled()) logger.info("[{}] reconnect: available services {}", registryClassName, availableServices);
            } finally {
                serverLock.unlock();
            }
        }
    }
}
