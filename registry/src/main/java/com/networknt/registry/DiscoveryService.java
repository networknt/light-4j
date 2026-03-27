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

import java.util.List;

/**
 *
 * Discovery service.
 *
 * @author fishermen
 */

public interface DiscoveryService {

    /**
     * Subscribes to changes for a given URL.
     *
     * @param url URL to subscribe to
     * @param listener NotifyListener to receive updates
     */
    void subscribe(URL url, NotifyListener listener);

    /**
     * Unsubscribes from changes for a given URL.
     *
     * @param url URL to unsubscribe from
     * @param listener NotifyListener to be removed
     */
    void unsubscribe(URL url, NotifyListener listener);

    /**
     * Discovers URLs for a given service URL.
     *
     * @param url service URL to discover
     * @return List of discovered URLs
     */
    List<URL> discover(URL url);
}
