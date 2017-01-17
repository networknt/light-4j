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
package com.networknt.registry.support;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.networknt.exception.FrameworkException;
import com.networknt.status.Status;
import com.networknt.registry.Registry;
import com.networknt.registry.RegistryFactory;
import com.networknt.registry.URL;

/**
 * 
 * Create and cache registry.
 * 
 * @author fishermen
 */

public abstract class AbstractRegistryFactory implements RegistryFactory {
    private static final String CREATE_REGISTRY_ERROR = "ERR10018";
    private static ConcurrentHashMap<String, Registry> registries = new ConcurrentHashMap<String, Registry>();

    private static final ReentrantLock lock = new ReentrantLock();

    protected String getRegistryUri(URL url) {
        String registryUri = url.getUri();
        return registryUri;
    }

    @Override
    public Registry getRegistry(URL url) {
        String registryUri = getRegistryUri(url);
        try {
            lock.lock();
            Registry registry = registries.get(registryUri);
            if (registry != null) {
                return registry;
            }
            registry = createRegistry(url);
            if (registry == null) {
                throw new FrameworkException(new Status(CREATE_REGISTRY_ERROR, url));
            }
            registries.put(registryUri, registry);
            return registry;
        } catch (Exception e) {
            throw new FrameworkException(new Status(CREATE_REGISTRY_ERROR, url), e);
        } finally {
            lock.unlock();
        }
    }

    protected abstract Registry createRegistry(URL url);
}
