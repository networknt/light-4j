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

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.consul.ConsulConfig;
import com.networknt.consul.ConsulConstants;
import com.networknt.consul.ConsulResponse;
import com.networknt.consul.ConsulService;
import com.networknt.httpstring.HttpStringConstants;
import com.networknt.utility.StringUtils;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A client that talks to Consul agent with REST API.
 * Client and connection are cached as instance variable in singleton class.
 *
 * @author Steve Hu
 */
public class ConsulClientImpl implements ConsulClient {
	private static final Logger logger = LoggerFactory.getLogger(ConsulClientImpl.class);
	private static final ConsulConfig config = (ConsulConfig)Config.getInstance().getJsonObjectConfig(ConsulConstants.CONFIG_NAME, ConsulConfig.class);
	private static AtomicInteger reqCounter = new AtomicInteger(0);
	private static final int UNUSUAL_STATUS_CODE = 300;

	// Single Factory Object so just like static.
	private Http2Client client = Http2Client.getInstance();
	// There is only one cached connection shared by all the API calls to Consul. If http is used, it should
	// be for dev testing only and one connection should be fine. For production, https should be used and it
	// supports multiplex.
	private ClientConnection connection;
	// if http2 is not enabled, connection should be cached within a connectionPool
	private ConcurrentHashMap<String, ConsulConnection> connectionPool = new ConcurrentHashMap<>();
	private OptionMap optionMap;
	private URI uri;
	private int maxReqPerConn;
	private String wait = "600s";

	/**
	 * Construct ConsulClient with all parameters from consul.yml config file. The other two constructors are
	 * just for backward compatibility.
	 *
	 */
	public ConsulClientImpl() {
		// Will use http/2 connection if tls is enabled as Consul only support HTTP/2 with TLS.
		String consulUrl = config.getConsulUrl().toLowerCase();
		optionMap =  config.isEnableHttp2() ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true) : OptionMap.EMPTY;
		logger.debug("url = {}", consulUrl);
		if(config.getWait() != null && config.getWait().length() > 2) wait = config.getWait();
		logger.debug("wait = {}", wait);
		try {
			uri = new URI(consulUrl);
		} catch (URISyntaxException e) {
			logger.error("Invalid URI " + consulUrl, e);
			throw new RuntimeException("Invalid URI " + consulUrl, e);
		}
		maxReqPerConn = config.getMaxReqPerConn() > 0 ? config.getMaxReqPerConn() : 1000000;
	}

	@Override
	public void checkPass(String serviceId, String token) {
		logger.debug("checkPass serviceId = {}", serviceId);
		String path = "/v1/agent/check/pass/" + "service:" + serviceId;
		try {
			final AtomicReference<ClientResponse> reference = send(Methods.PUT,path, token, null);
			int statusCode = reference.get().getResponseCode();
			if(statusCode >= UNUSUAL_STATUS_CODE){
				logger.error("Failed to checkPass on Consul: {} : {}", statusCode, reference.get().getAttachment(Http2Client.RESPONSE_BODY));
				throw new Exception("Failed to checkPass on Consul: " + statusCode + ":" + reference.get().getAttachment(Http2Client.RESPONSE_BODY));
			}
		} catch (Exception e) {
			logger.error("CheckPass request exception", e);
		}
	}

	@Override
	public void checkFail(String serviceId, String token) {
		logger.debug("checkFail serviceId = {}", serviceId);
		String path = "/v1/agent/check/fail/" + "service:" + serviceId;
		try {
			AtomicReference<ClientResponse> reference = send(Methods.PUT, path, token, null);
			int statusCode = reference.get().getResponseCode();
			if(statusCode >= UNUSUAL_STATUS_CODE){
				logger.error("Failed to checkFail on Consul: {} : {}", statusCode, reference.get().getAttachment(Http2Client.RESPONSE_BODY));
				throw new Exception("Failed to checkFail on Consul: " + statusCode + ":" + reference.get().getAttachment(Http2Client.RESPONSE_BODY));
			}
		} catch (Exception e) {
			logger.error("CheckFail request exception", e);
		}
	}

	@Override
	public void registerService(ConsulService service, String token) {
		String json = service.toString();
		String path = "/v1/agent/service/register";
		try {
			AtomicReference<ClientResponse> reference = send(Methods.PUT, path, token, json);
			int statusCode = reference.get().getResponseCode();
			if(statusCode >= UNUSUAL_STATUS_CODE){
				throw new Exception("Failed to register on Consul: " + statusCode);
			}
		} catch (Exception e) {
			logger.error("Failed to register on Consul, Exception:", e);
			throw new RuntimeException(e.getMessage());
		}
	}

	@Override
	public void unregisterService(String serviceId, String token) {
		try {
            String path = "/v1/agent/service/deregister/" + serviceId;
            final AtomicReference<ClientResponse> reference = send(Methods.PUT, path, token, null);
            int statusCode = reference.get().getResponseCode();
            if(statusCode >= UNUSUAL_STATUS_CODE){
                logger.error("Failed to unregister on Consul, body = {}", reference.get().getAttachment(Http2Client.RESPONSE_BODY));
            }
		} catch (Exception e) {
			logger.error("Failed to unregister on Consul, Exception:", e);
		}
	}

	@Override
	public ConsulResponse<List<ConsulService>> lookupHealthService(String serviceName, String tag, long lastConsulIndex, String token) {
		ClientConnection connection = getConnection(serviceName);

		ConsulResponse<List<ConsulService>> newResponse = null;

		String path = "/v1/health/service/" + serviceName + "?passing&wait="+wait+"&index=" + lastConsulIndex;
		if(tag != null) {
			path = path + "&tag=" + tag;
		}
		logger.debug("path = {}", path);
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<ClientResponse> reference = new AtomicReference<>();
		try {
			ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(path);
			if (token != null) request.getRequestHeaders().put(HttpStringConstants.CONSUL_TOKEN, token);
			request.getRequestHeaders().put(Headers.HOST, "localhost");
			logger.debug("The request sent to consul: {} = request header: {}, request body is empty", uri.toString(), request.toString());
            connection.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await();
			reqCounter.getAndIncrement();
			logger.debug("The response got from consul: {} = {}", uri.toString(), reference.get().toString());
			int statusCode = reference.get().getResponseCode();
			if(statusCode >= UNUSUAL_STATUS_CODE){
				throw new Exception("Failed to unregister on Consul: " + statusCode);
			} else {
				String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
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

	private ClientConnection createConnection() {
		logger.debug("connection is closed with counter {}, reconnecting...", reqCounter);
		ClientConnection newConnection = null;
		try {
			newConnection = client.connect(uri, Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, optionMap).get();
		} catch (IOException e) {
			logger.error("cannot create connection to consul {} due to: {}", uri, e.getMessage());
		}
		reqCounter = new AtomicInteger(0);
		return newConnection;
	}

	/**
	 * if http2 is enabled, try to establish connect and return the class level ClientConnection
	 * if http2 is disabled, try to get from connection pool, and return from the CachedConnection
	 * @param serviceName service name as the key for caching connection
	 * @return ClientConnection
	 */
	private ClientConnection getConnection(String serviceName) {
		if(config.isEnableHttp2()) {
			if(needsToCreateConnection(this.connection)) {
				this.connection = createConnection();
			}
			return this.connection;
		} else {
			ConsulConnection cachedConsulConnection = connectionPool.get(serviceName);
			ClientConnection cachedConnection = cachedConsulConnection == null ? null : cachedConsulConnection.getConnection();
			if(needsToCreateConnection(cachedConnection)) {
				cachedConnection = createConnection();
				//when init the connection or needs to reconnect, update connection pool
				connectionPool.put(serviceName, new ConsulConnection(cachedConnection, new AtomicInteger(0)));
			}
			return cachedConnection;
		}
	}

	private boolean needsToCreateConnection(ClientConnection connection) {
		return connection == null || !connection.isOpen() || reqCounter.get() >= maxReqPerConn;
	}

	/**
	 * send to consul
	 * @param path path to send to consul
	 * @param token token to put in header
	 * @param json request body
	 * @return AtomicReference<ClientResponse> response
	 */
	private AtomicReference<ClientResponse> send (HttpString method, String path, String token, String json) {
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<ClientResponse> reference = new AtomicReference<>();
		if (needsToCreateConnection(connection)) {
			this.connection = createConnection();
		}

		ClientRequest request = new ClientRequest().setMethod(method).setPath(path);
		request.getRequestHeaders().put(Headers.HOST, "localhost");
		if (token != null) request.getRequestHeaders().put(HttpStringConstants.CONSUL_TOKEN, token);
		logger.debug("The request sent to consul: {} = request header: {}, request body is empty", uri.toString(), request.toString());
		if(StringUtils.isBlank(json)) {
			connection.sendRequest(request, client.createClientCallback(reference, latch));
		} else {
			connection.sendRequest(request, client.createClientCallback(reference, latch, json));
		}
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		reqCounter.getAndIncrement();
		logger.debug("The response got from consul: {} = {}", uri.toString(), reference.get().toString());
		return reference;
	}

	private class ConsulConnection {
		private ClientConnection connection;
		private AtomicInteger reqCounter;

		public ConsulConnection(ClientConnection connection, AtomicInteger reqCounter) {
			this.connection = connection;
			this.reqCounter = reqCounter;
		}

		public ClientConnection getConnection() {
			return connection;
		}

		public void setConnection(ClientConnection connection) {
			this.connection = connection;
		}

		public AtomicInteger getReqCounter() {
			return reqCounter;
		}

		public void setReqCounter(AtomicInteger reqCounter) {
			this.reqCounter = reqCounter;
		}
	}
}
