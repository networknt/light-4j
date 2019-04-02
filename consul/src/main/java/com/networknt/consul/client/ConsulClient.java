/*
 * Copyright (c) 2016 Network New Technologies Inc.
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

package com.networknt.consul.client;

import java.util.List;

import com.networknt.consul.ConsulResponse;
import com.networknt.consul.ConsulService;

public interface ConsulClient {

	/**
	 * Set specific serviceId status as pass
	 *
	 * @param serviceId service id
	 * @param token ACL token for consul
	 */
	void checkPass(String serviceId, String token);

	/**
	 * Set specific serviceId status as fail
	 *
	 * @param serviceId service id
	 * @param token ACL token for consul
	 */
	void checkFail(String serviceId, String token);

	/**
	 * register a consul service
	 *
	 * @param service service object
	 * @param token ACL token for consul
	 */
	void registerService(ConsulService service, String token);

	/**
	 * unregister a consul service
	 *
	 * @param serviceid service id
	 * @param token ACL token for consul
	 */
	void unregisterService(String serviceid, String token);

	/**
	 * get latest service list with a tag filter and a security token
	 *
	 * @param serviceName service name
	 * @param lastConsulIndex last consul index
	 * @param tag tag that is used for filtering
	 * @param token consul token for security
	 * @return T
	 */
	ConsulResponse<List<ConsulService>> lookupHealthService(String serviceName, String tag, long lastConsulIndex, String token);

}
