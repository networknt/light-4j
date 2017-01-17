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

import com.networknt.registry.URLParamType;
import com.networknt.registry.Registry;
import com.networknt.registry.support.AbstractRegistryFactory;
import com.networknt.registry.URL;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZookeeperRegistryFactory extends AbstractRegistryFactory {
    private static final Logger logger = LoggerFactory.getLogger(ZookeeperRegistryFactory.class);
    @Override
    protected Registry createRegistry(URL registryUrl) {
        try {
            int timeout = registryUrl.getIntParameter(URLParamType.connectTimeout.getName(), URLParamType.connectTimeout.getIntValue());
            int sessionTimeout =
                    registryUrl.getIntParameter(URLParamType.registrySessionTimeout.getName(),
                            URLParamType.registrySessionTimeout.getIntValue());
            ZkClient zkClient = new ZkClient(registryUrl.getParameter("address"), sessionTimeout, timeout);
            return new ZookeeperRegistry(registryUrl, zkClient);
        } catch (ZkException e) {
            logger.error("[ZookeeperRegistry] fail to connect zookeeper, cause: " + e.getMessage());
            throw e;
        }
    }
}
