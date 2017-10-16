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
