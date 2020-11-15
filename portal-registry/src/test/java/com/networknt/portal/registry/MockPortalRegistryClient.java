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

package com.networknt.portal.registry;

import com.networknt.portal.registry.client.PortalRegistryClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author zhanglei28
 * @Description MockConsulClient
 */
public class MockPortalRegistryClient implements PortalRegistryClient {
    // save mock service heart beat
    private ConcurrentHashMap<String, AtomicLong> checkPassTimesMap = new ConcurrentHashMap<String, AtomicLong>();

    // registered service and service status, true is available, false unavailable
    private ConcurrentHashMap<String, Boolean> serviceStatus = new ConcurrentHashMap<String, Boolean>();
    // registered serviceId and service relationship
    private ConcurrentHashMap<String, PortalRegistryService> services = new ConcurrentHashMap<String, PortalRegistryService>();
    // KVValue
    private ConcurrentHashMap<String, String> KVValues = new ConcurrentHashMap<String, String>();

    String host;
    int port;

    public MockPortalRegistryClient(String host, int port) {
        this.host = host;
        this.port = port;
    }


    @Override
    public void checkPass(PortalRegistryService service, String token) {
        AtomicLong times = checkPassTimesMap.get(service.getServiceId() + service.getAddress() + service.getPort());
        if (times == null) {
            checkPassTimesMap.putIfAbsent(service.getServiceId() + service.getAddress() + service.getPort(), new AtomicLong());
            times = checkPassTimesMap.get(service.getServiceId() + service.getAddress() + service.getPort());
        }
        times.getAndIncrement();

        serviceStatus.put(service.getServiceId() + service.getAddress() + service.getPort(), true);
    }

    @Override
    public void checkFail(PortalRegistryService service, String tag) {
        serviceStatus.put(service.getServiceId() + service.getAddress() + service.getPort(), false);
    }

    @Override
    public void registerService(PortalRegistryService service, String token) {
        serviceStatus.put(service.getServiceId() + service.getAddress() + service.getPort(), false);
        services.put(service.getServiceId() + service.getAddress() + service.getPort(), service);
    }

    @Override
    public void unregisterService(PortalRegistryService service, String token) {
        serviceStatus.remove(service.getServiceId() + service.getAddress() + service.getPort());
        services.remove(service.getServiceId() + service.getAddress() + service.getPort());
    }

    @Override
    public PortalRegistryResponse<List<PortalRegistryService>> lookupHealthService(String serviceName, String tag, String token) {
        PortalRegistryResponse<List<PortalRegistryService>> res = new PortalRegistryResponse<List<PortalRegistryService>>();
        List<PortalRegistryService> list = new ArrayList<PortalRegistryService>();
        for (Map.Entry<String, Boolean> entry : serviceStatus.entrySet()) {
            if (entry.getValue()) {
                list.add(services.get(entry.getKey()));
            }
        }
        res.setValue(list);
        return res;
    }

    public long getCheckPassTimes(PortalRegistryService service) {
        AtomicLong times = checkPassTimesMap.get(service.getServiceId() + service.getAddress() + service.getPort());
        if (times == null) {
            return 0;
        }
        return times.get();
    }

    public boolean isRegistered(PortalRegistryService service) {
        return serviceStatus.containsKey(service.getServiceId() + service.getAddress() + service.getPort());
    }

    public boolean isWorking(PortalRegistryService service) {
        return serviceStatus.get(service.getServiceId() + service.getAddress() + service.getPort());
    }

    public void removeService(PortalRegistryService service) {
        serviceStatus.remove(service.getServiceId() + service.getAddress() + service.getPort());
        services.remove(service.getServiceId() + service.getAddress() + service.getPort());
    }
}
