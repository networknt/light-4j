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
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A client that talks to Consul agent with REST API.
 * Client and connection are cached as instance variable in singleton class.
 *
 * @author Steve Hu
 */
public class ConsulClientImpl implements ConsulClient {
	private static final Logger logger = LoggerFactory.getLogger(ConsulClientImpl.class);
	// Single Factory Object so just like static.
	Http2Client client = Http2Client.getInstance();
	// There is only one cached connection shared by all the API calls to Consul. If http is used, it should
	// be for dev testing only and one connection should be fine. For production, https must be used and it
	// supports multiplex.
	ClientConnection connection;
	OptionMap optionMap;
	URI uri;

	/**
	 * Construct ConsulClient with protocol, host and port.
	 *
	 * @param protocol protocol
	 * @param host host
	 * @param port port
	 */
	public ConsulClientImpl(String protocol, String host, int port) {
		// Will use http/2 connection if tls is enabled as Consul only support HTTP/2 with TLS.
		optionMap =  "https".equalsIgnoreCase(protocol) ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true) : OptionMap.EMPTY;
		String url = protocol + "://" + host + ":" + port;
		if(logger.isDebugEnabled()) logger.debug("url = " + url);
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			logger.error("Invalid URI " + url, e);
			throw new RuntimeException("Invalid URI " + url, e);
		}
	}

	/**
	 * Construct ConsulClient with protocol and host.
	 *
	 * @param protocol protocol
	 * @param host host
	 */
	public ConsulClientImpl(String protocol, String host) {
		// Will use http/2 connection if tls is enabled as Consul only support HTTP/2 with TLS.
		optionMap =  "https".equalsIgnoreCase(protocol) ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true) : OptionMap.EMPTY;
		String url = protocol + "://" + host;
		if(logger.isDebugEnabled()) logger.debug("url = " + url);
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			logger.error("Invalid URI " + url, e);
			throw new RuntimeException("Invalid URI " + url, e);
		}
	}

	@Override
	public void checkPass(String serviceId, String token) {
		if(logger.isDebugEnabled()) logger.debug("checkPass serviceId = " + serviceId);
		String path = "/v1/agent/check/pass/" + "service:" + serviceId;
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<ClientResponse> reference = new AtomicReference<>();
		try {
			if(connection == null || !connection.isOpen()) {
				if(logger.isDebugEnabled()) logger.debug("connection is closed somehow, reconnecting...");
				connection = client.connect(uri, Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL, optionMap).get();
			}
			ClientRequest request = new ClientRequest().setMethod(Methods.PUT).setPath(path);
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
		}
	}

	@Override
	public void checkFail(String serviceId, String token) {
		if(logger.isDebugEnabled()) logger.debug("checkFail serviceId = " + serviceId);
		String path = "/v1/agent/check/fail/" + "service:" + serviceId;
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<ClientResponse> reference = new AtomicReference<>();
		try {
			if(connection == null || !connection.isOpen()) {
				if(logger.isDebugEnabled()) logger.debug("connection is closed somehow, reconnecting...");
				connection = client.connect(uri, Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL, optionMap).get();
			}
			ClientRequest request = new ClientRequest().setMethod(Methods.PUT).setPath(path);
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
		}
	}

	@Override
	public void registerService(ConsulService service, String token) {
		String json = service.toString();
		String path = "/v1/agent/service/register";
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<ClientResponse> reference = new AtomicReference<>();
		try {
			if(connection == null || !connection.isOpen()) {
				if(logger.isDebugEnabled()) logger.debug("connection is closed somehow, reconnecting...");
				connection = client.connect(uri, Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL, optionMap).get();
			}
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
		}
	}

	@Override
	public void unregisterService(String serviceId, String token) {
		String path = "/v1/agent/service/deregister/" + serviceId;
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<ClientResponse> reference = new AtomicReference<>();
		try {
			if(connection == null || !connection.isOpen()) {
				if(logger.isDebugEnabled()) logger.debug("connection is closed somehow, reconnecting...");
				connection = client.connect(uri, Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL, optionMap).get();
			}

			ClientRequest request = new ClientRequest().setMethod(Methods.PUT).setPath(path);
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
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<ClientResponse> reference = new AtomicReference<>();
		try {
			if(connection == null || !connection.isOpen()) {
				if(logger.isDebugEnabled()) logger.debug("connection is closed somehow, reconnecting...");
				connection = client.connect(uri, Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL, optionMap).get();
			}
			ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(path);
			if(token != null) request.getRequestHeaders().put(Constants.CONSUL_TOKEN, token);
			request.getRequestHeaders().put(Headers.HOST, "localhost");
			connection.sendRequest(request, client.createClientCallback(reference, latch));
			latch.await();
			int statusCode = reference.get().getResponseCode();
			if(statusCode >= 300){
				if(logger.isDebugEnabled()) logger.debug("body = " + reference.get().getAttachment(Http2Client.RESPONSE_BODY));
				throw new Exception("Failed to unregister on Consul: " + statusCode);
			} else {
				String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
				if(logger.isDebugEnabled()) logger.debug("body = " + body);
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
