package com.networknt.consul.client;

import java.util.List;

import com.networknt.consul.ConsulResponse;
import com.networknt.consul.ConsulService;

public abstract class ConsulClient {

	protected String host;

	protected int port;

	public ConsulClient(String host, int port) {
		super();
		this.host = host;
		this.port = port;
	}

	/**
	 * Set specific serviceId status as pass
	 *
	 * @param serviceId service id
	 */
	public abstract void checkPass(String serviceId);

	/**
	 * Set specific serviceId status as fail
	 *
	 * @param serviceId service id
	 */
	public abstract void checkFail(String serviceId);

	/**
	 * register a consul service
	 *
	 * @param service service object
	 */
	public abstract void registerService(ConsulService service);

	/**
	 * unregister a consul service
	 *
	 * @param serviceid service id
	 */
	public abstract void unregisterService(String serviceid);

	/**
	 * 获取最新的可用服务列表。
	 *
	 * @param serviceName service name
	 * @param lastConsulIndex last consul index
	 * @return T
	 */
	public abstract ConsulResponse<List<ConsulService>> lookupHealthService(
			String serviceName, long lastConsulIndex);

	public abstract String lookupCommand(String group);

}
