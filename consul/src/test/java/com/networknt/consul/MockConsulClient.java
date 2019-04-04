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

package com.networknt.consul;

import com.networknt.consul.client.ConsulClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author zhanglei28
 * @Description MockConsulClient
 */
public class MockConsulClient implements ConsulClient {
    // save mock service heart beat
    private ConcurrentHashMap<String, AtomicLong> checkPassTimesMap = new ConcurrentHashMap<String, AtomicLong>();

    // registered service and service status, true is available, false unavailable
    private ConcurrentHashMap<String, Boolean> serviceStatus = new ConcurrentHashMap<String, Boolean>();
    // registered serviceId and service relationship
    private ConcurrentHashMap<String, ConsulService> services = new ConcurrentHashMap<String, ConsulService>();
    // KVValue
    private ConcurrentHashMap<String, String> KVValues = new ConcurrentHashMap<String, String>();

    private int mockServiceNum = 10;
    private long mockIndex = 10;

    String host;
    int port;

    public MockConsulClient(String host, int port) {
        this.host = host;
        this.port = port;
    }


    @Override
    public void checkPass(String serviceid, String token) {
        AtomicLong times = checkPassTimesMap.get(serviceid);
        if (times == null) {
            checkPassTimesMap.putIfAbsent(serviceid, new AtomicLong());
            times = checkPassTimesMap.get(serviceid);
        }
        times.getAndIncrement();

        serviceStatus.put(serviceid, true);
    }

    @Override
    public void checkFail(String serviceid, String tag) {
        serviceStatus.put(serviceid, false);
    }

    @Override
    public void registerService(ConsulService service, String token) {
        serviceStatus.put(service.getId(), false);
        services.put(service.getId(), service);
    }

    @Override
    public void unregisterService(String serviceid, String token) {
        serviceStatus.remove(serviceid);
        services.remove(serviceid);
    }

    @Override
    public ConsulResponse<List<ConsulService>> lookupHealthService(String serviceName, String tag, long lastConsulIndex, String token) {
        ConsulResponse<List<ConsulService>> res = new ConsulResponse<List<ConsulService>>();
        res.setConsulIndex(lastConsulIndex + 1);
        res.setConsulKnownLeader(true);
        res.setConsulLastContact(0L);

        List<ConsulService> list = new ArrayList<ConsulService>();
        for (Map.Entry<String, Boolean> entry : serviceStatus.entrySet()) {
            if (entry.getValue()) {
                list.add(services.get(entry.getKey()));
            }
        }
        res.setValue(list);
        return res;
    }

    public long getCheckPassTimes(String serviceid) {
        AtomicLong times = checkPassTimesMap.get(serviceid);
        if (times == null) {
            return 0;
        }
        return times.get();
    }

    public int getMockServiceNum() {
        return mockServiceNum;
    }

    public void setMockServiceNum(int mockServiceNum) {
        this.mockServiceNum = mockServiceNum;
    }

    public boolean isRegistered(String serviceid) {
        return serviceStatus.containsKey(serviceid);
    }

    public boolean isWorking(String serviceid) {
        return serviceStatus.get(serviceid);
    }

    public void removeService(String serviceid) {
        serviceStatus.remove(serviceid);
        services.remove(serviceid);
    }

    public long getMockIndex() {
        return mockIndex;
    }

    public void setKVValue(String key, String value) {
        KVValues.put(key, value);
    }

    public void removeKVValue(String key) {
        KVValues.remove(key);
    }

}
