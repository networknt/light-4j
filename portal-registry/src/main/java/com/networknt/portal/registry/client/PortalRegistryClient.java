/*
 * Copyright (c) 2020 Network New Technologies Inc.
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
package com.networknt.portal.registry.client;

import java.util.List;

import com.networknt.portal.registry.PortalRegistry;
import com.networknt.portal.registry.PortalRegistryResponse;
import com.networknt.portal.registry.PortalRegistryService;

public interface PortalRegistryClient {

	/**
	 * Set specific serviceId status as pass
	 *
	 * @param service PortalRegistryService
	 * @param token ACL token for consul
	 */
	void checkPass(PortalRegistryService service, String token);

	/**
	 * Set specific serviceId status as fail
	 *
	 * @param service PortalRegistryService
	 * @param token ACL token for consul
	 */
	void checkFail(PortalRegistryService service, String token);

	/**
	 * register a consul service
	 *
	 * @param service service object
	 * @param token A bootstrap JWT token to access portal controller
	 */
	void registerService(PortalRegistryService service, String token);

	/**
	 * unregister a consul service
	 *
	 * @param service service object
	 * @param token bootstrap JWT token to access portal controller
	 */
	void unregisterService(PortalRegistryService service, String token);

	/**
	 * get latest service list with a tag filter and a security token
	 *
	 * @param serviceId serviceId
	 * @param tag tag that is used for filtering
	 * @param token consul token for security
	 * @return T
	 */
	PortalRegistryResponse<List<PortalRegistryService>> lookupHealthService(String serviceId, String tag, String token);

}
