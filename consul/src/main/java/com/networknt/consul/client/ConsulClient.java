package com.networknt.consul.client;

import java.util.List;

import com.networknt.consul.ConsulResponse;
import com.networknt.consul.ConsulService;

public interface ConsulClient {

	/**
	 * Set specific serviceId status as pass
	 *
	 * @param serviceId service id
	 */
	void checkPass(String serviceId);

	/**
	 * Set specific serviceId status as fail
	 *
	 * @param serviceId service id
	 */
	void checkFail(String serviceId);

	/**
	 * register a consul service
	 *
	 * @param service service object
	 */
	void registerService(ConsulService service);

	/**
	 * unregister a consul service
	 *
	 * @param serviceid service id
	 */
	void unregisterService(String serviceid);

	/**
	 * get latest service list
	 *
	 * @param serviceName service name
	 * @param lastConsulIndex last consul index
	 * @return T
	 */
	ConsulResponse<List<ConsulService>> lookupHealthService(
			String serviceName, long lastConsulIndex);

}
