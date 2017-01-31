package com.networknt.consul.client;

import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.health.model.HealthService;
import com.ecwid.consul.v1.health.model.HealthService.Service;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.networknt.consul.ConsulConstants;
import com.networknt.consul.ConsulResponse;
import com.networknt.consul.ConsulService;
import com.networknt.consul.ConsulUtils;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ConsulEcwidClient implements ConsulClient {
	private static final Logger logger = LoggerFactory.getLogger(ConsulEcwidClient.class);

	public static com.ecwid.consul.v1.ConsulClient client;

	String host;
	int port;

	public ConsulEcwidClient(String host, int port) {
		this.host = host;
		this.port = port;

		client = new com.ecwid.consul.v1.ConsulClient(host, port);
		if(logger.isInfoEnabled()) logger.info("ConsulEcwidClient init finish. client host:" + host
				+ ", port:" + port);
	}

	@Override
	public void checkPass(String serviceId) {
		if(logger.isDebugEnabled()) logger.debug("checkPass serviceId = " + serviceId);
		client.agentCheckPass("service:" + serviceId);
	}

	@Override
	public void registerService(ConsulService service) {
		NewService newService = convertService(service);
		client.agentServiceRegister(newService);
	}

	@Override
	public void unregisterService(String serviceid) {
		client.agentServiceDeregister(serviceid);
	}

	@Override
	public ConsulResponse<List<ConsulService>> lookupHealthService(
			String serviceName, long lastConsulIndex) {
		QueryParams queryParams = new QueryParams(
				ConsulConstants.CONSUL_BLOCK_TIME_SECONDS, lastConsulIndex);
		Response<List<HealthService>> orgResponse = client.getHealthServices(
				serviceName, true, queryParams);
		ConsulResponse<List<ConsulService>> newResponse = null;
		if (orgResponse != null && orgResponse.getValue() != null
				&& !orgResponse.getValue().isEmpty()) {
			List<HealthService> HealthServices = orgResponse.getValue();
			List<ConsulService> ConsulServcies = new ArrayList<ConsulService>(
					HealthServices.size());

			for (HealthService orgService : HealthServices) {
				try {
					ConsulService newService = convertToConsulService(orgService);
					ConsulServcies.add(newService);
				} catch (Exception e) {
					String servcieid = "null";
					if (orgService.getService() != null) {
						servcieid = orgService.getService().getId();
					}
					logger.error(
							"convert consul service fail. org consulservice:"
									+ servcieid, e);
				}
			}
			if (!ConsulServcies.isEmpty()) {
				newResponse = new ConsulResponse<List<ConsulService>>();
				newResponse.setValue(ConsulServcies);
				newResponse.setConsulIndex(orgResponse.getConsulIndex());
				newResponse.setConsulLastContact(orgResponse
						.getConsulLastContact());
				newResponse.setConsulKnownLeader(orgResponse
						.isConsulKnownLeader());
			}
		}

		return newResponse;
	}

    @Override
    public String lookupCommand(String group) {
        Response<GetValue> response = client.getKVValue(ConsulConstants.CONSUL_LIGHT_COMMAND + ConsulUtils.convertGroupToServiceName(group));
        GetValue value = response.getValue();
        String command = "";
        if (value == null) {
            if(logger.isInfoEnabled()) logger.info("no command in group: " + group);
        } else if (value.getValue() != null) {
            command = new String(Base64.decodeBase64(value.getValue()), UTF_8);
        }
        return command;
    }

	private NewService convertService(ConsulService service) {
		NewService newService = new NewService();
		newService.setAddress(service.getAddress());
		newService.setId(service.getId());
		newService.setName(service.getName());
		newService.setPort(service.getPort());
		newService.setTags(service.getTags());

		NewService.Check check = new NewService.Check();
		check.setTtl(service.getTtl() + "s");
		newService.setCheck(check);

		return newService;
	}

	private ConsulService convertToConsulService(HealthService healthService) {
		ConsulService service = new ConsulService();
		Service org = healthService.getService();
		service.setAddress(org.getAddress());
		service.setId(org.getId());
		service.setName(org.getService());
		service.setPort(org.getPort());
		service.setTags(org.getTags());
		return service;
	}

	@Override
	public void checkFail(String serviceId) {
		if(logger.isDebugEnabled()) logger.debug("checkFail serviceId = " + serviceId);
		client.agentCheckFail("service:" + serviceId);
	}

}
