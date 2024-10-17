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

package com.networknt.registry;

/**
 *
 * Used to register and discover.
 *
 * @author fishermen
 */
public interface Registry extends RegistryService, DiscoveryService {

    URL getUrl();

    /**
     * get the serviceKey based on serviceId and tag for discovery cache.
     * @param serviceId service id
     * @param tag service tag
     * @return key that is combined serviceId and tag.
     */
    default String serviceKey(String serviceId, String tag) {
        return tag == null ? serviceId : serviceId + "|" + tag;
    }

}
