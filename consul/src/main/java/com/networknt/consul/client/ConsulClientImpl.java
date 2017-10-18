package com.networknt.consul.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.consul.ConsulResponse;
import com.networknt.consul.ConsulService;
import com.networknt.utility.Constants;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class ConsulClientImpl implements ConsulClient {
	private static final Logger logger = LoggerFactory.getLogger(ConsulClientImpl.class);
	static Http2Client client = Http2Client.getInstance();
	String url;

	/**
	 * Construct ConsulClient with protocol, host and port.
	 *
	 * @param protocol protocol
	 * @param host host
	 * @param port port
	 */
	public ConsulClientImpl(String protocol, String host, int port) {
		url = protocol + "://" + host + ":" + port;
	}

	/**
	 * Construct ConsulClient with protocol and host.
	 *
	 * @param protocol protocol
	 * @param host host
	 */
	public ConsulClientImpl(String protocol, String host) {
		url = protocol + "://" + host;
	}

	@Override
	public void checkPass(String serviceId, String token) {
		if(logger.isDebugEnabled()) logger.debug("checkPass serviceId = " + serviceId);
		String path = "/v1/agent/check/pass/" + "service:" + serviceId;
		ClientConnection connection = null;
		try {
			connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL, OptionMap.EMPTY).get();
		} catch (Exception e) {
			logger.error("Exeption:", e);
		}
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<ClientResponse> reference = new AtomicReference<>();
		try {
			ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(path);
			request.getRequestHeaders().put(Headers.HOST, "localhost");
			if(token != null) request.getRequestHeaders().put(Constants.CONSUL_TOKEN, token);
			connection.sendRequest(request, client.createClientCallback(reference, latch));
			latch.await();
			int statusCode = reference.get().getResponseCode();
			if(statusCode >= 300){
				logger.error("Failed to checkPass on Consul: " + statusCode + ":" + reference.get().getAttachment(Http2Client.RESPONSE_BODY));
				throw new Exception("Failed to checkPass on Consul: " + statusCode + ":" + reference.get().getAttachment(Http2Client.RESPONSE_BODY));
			}
		} catch (Exception e) {
			logger.error("CheckPass request exception", e);
		} finally {
			IoUtils.safeClose(connection);
		}

	}

	@Override
	public void checkFail(String serviceId, String token) {
		if(logger.isDebugEnabled()) logger.debug("checkFail serviceId = " + serviceId);
		String path = "/v1/agent/check/fail/" + "service:" + serviceId;
		ClientConnection connection = null;
		try {
			connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL, OptionMap.EMPTY).get();
		} catch (Exception e) {
			logger.error("Exeption:", e);
		}
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<ClientResponse> reference = new AtomicReference<>();
		try {
			ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(path);
			request.getRequestHeaders().put(Headers.HOST, "localhost");
			if(token != null) request.getRequestHeaders().put(Constants.CONSUL_TOKEN, token);
			connection.sendRequest(request, client.createClientCallback(reference, latch));
			latch.await();
			int statusCode = reference.get().getResponseCode();
			if(statusCode >= 300){
				logger.error("Failed to checkPass on Consul: " + statusCode + ":" + reference.get().getAttachment(Http2Client.RESPONSE_BODY));
				throw new Exception("Failed to checkPass on Consul: " + statusCode + ":" + reference.get().getAttachment(Http2Client.RESPONSE_BODY));
			}
		} catch (Exception e) {
			logger.error("CheckPass request exception", e);
		} finally {
			IoUtils.safeClose(connection);
		}
	}

	@Override
	public void registerService(ConsulService service, String token) {
		String json = service.toString();
		String path = "/v1/agent/service/register";
		ClientConnection connection = null;
		try {
			connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL, OptionMap.EMPTY).get();
		} catch (Exception e) {
			logger.error("Exeption:", e);
		}
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<ClientResponse> reference = new AtomicReference<>();
		try {
			ClientRequest request = new ClientRequest().setMethod(Methods.PUT).setPath(path);
			if(token != null) request.getRequestHeaders().put(Constants.CONSUL_TOKEN, token);
			request.getRequestHeaders().put(Headers.HOST, "localhost");
			request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
			connection.sendRequest(request, client.createClientCallback(reference, latch, json));
			latch.await();
			int statusCode = reference.get().getResponseCode();
			if(statusCode >= 300){
				throw new Exception("Failed to register on Consul: " + statusCode);
			}
		} catch (Exception e) {
			logger.error("Exception:", e);
		} finally {
			IoUtils.safeClose(connection);
		}
	}

	@Override
	public void unregisterService(String serviceId, String token) {
		String path = "/v1/agent/service/deregister/" + serviceId;
		ClientConnection connection = null;
		try {
			connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL, OptionMap.EMPTY).get();
		} catch (Exception e) {
			logger.error("Exeption:", e);
		}
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<ClientResponse> reference = new AtomicReference<>();
		try {
			ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(path);
            request.getRequestHeaders().put(Headers.HOST, "localhost");
			if(token != null) request.getRequestHeaders().put(Constants.CONSUL_TOKEN, token);
			connection.sendRequest(request, client.createClientCallback(reference, latch));
			latch.await();
			int statusCode = reference.get().getResponseCode();
			if(statusCode >= 300){
				System.out.println("body = " + reference.get().getAttachment(Http2Client.RESPONSE_BODY));
				throw new Exception("Failed to unregister on Consul: " + statusCode);
			}
		} catch (Exception e) {
			logger.error("Exception:", e);
		} finally {
			IoUtils.safeClose(connection);
		}
	}

	@Override
	public ConsulResponse<List<ConsulService>> lookupHealthService(String serviceName, String tag, long lastConsulIndex, String token) {
		ConsulResponse<List<ConsulService>> newResponse = null;

		String path = "/v1/health/service/" + serviceName + "?passing&wait=600s&index=" + lastConsulIndex;
		if(tag != null) {
			path = path + "&tag=" + tag;
		}
		if(logger.isDebugEnabled()) logger.debug("path = " + path);
		ClientConnection connection = null;
		try {
			connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL, OptionMap.EMPTY).get();
		} catch (Exception e) {
			logger.error("Exeption:", e);
		}
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<ClientResponse> reference = new AtomicReference<>();
		try {
			ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(path);
			if(token != null) request.getRequestHeaders().put(Constants.CONSUL_TOKEN, token);
			request.getRequestHeaders().put(Headers.HOST, "localhost");
			connection.sendRequest(request, client.createClientCallback(reference, latch));
			latch.await();
			int statusCode = reference.get().getResponseCode();
			if(statusCode >= 300){
				System.out.println("body = " + reference.get().getAttachment(Http2Client.RESPONSE_BODY));
				throw new Exception("Failed to unregister on Consul: " + statusCode);
			} else {
				String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
				System.out.println("body = " + body);
				List<Map<String, Object>> services = Config.getInstance().getMapper().readValue(body, new TypeReference<List<Map<String, Object>>>(){});
				List<ConsulService> ConsulServcies = new ArrayList<>(
						services.size());
				for (Map<String, Object> service : services) {
					ConsulService newService = convertToConsulService((Map<String,Object>)service.get("Service"));
					ConsulServcies.add(newService);
				}
				if (!ConsulServcies.isEmpty()) {
					newResponse = new ConsulResponse<>();
					newResponse.setValue(ConsulServcies);
					newResponse.setConsulIndex(Long.parseLong(reference.get().getResponseHeaders().getFirst("X-Consul-Index")));
					newResponse.setConsulLastContact(Long.parseLong(reference.get().getResponseHeaders().getFirst("X-Consul-Lastcontact")));
					newResponse.setConsulKnownLeader(Boolean.parseBoolean(reference.get().getResponseHeaders().getFirst("X-Consul-Knownleader")));
				}
			}
		} catch (Exception e) {
			logger.error("Exception:", e);
		} finally {
			IoUtils.safeClose(connection);
		}
		return newResponse;
	}

	private ConsulService convertToConsulService(Map<String, Object> serviceMap) {
		ConsulService service = new ConsulService();
		service.setAddress((String)serviceMap.get("Address"));
		service.setId((String)serviceMap.get("ID"));
		service.setName((String)serviceMap.get("Service"));
		service.setPort((Integer)serviceMap.get("Port"));
		service.setTags((List)serviceMap.get("Tags"));
		return service;
	}

}
