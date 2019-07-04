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
	private static final int UNUSUAL_STATUS_CODE = 300;
	private Http2Client client = Http2Client.getInstance();

	/**
	 * if http2 is not enabled, connection should be cached within a connectionPool
	 */
	private ConcurrentHashMap<String, ConsulConnection> connectionPool = new ConcurrentHashMap<>();

	/**
	 *  Consul supports HTTP2 connection when using HTTPS, thus, there will be only one cached connection shared by all the API calls to Consul.
	 *  In production, https should be used and it supports multiplex.
	 */
	private ConsulConnection http2Connection = new ConsulConnection();
	private OptionMap optionMap;
	private URI uri;
	private int maxReqPerConn;
	private String wait = "600s";

	private final String REGISTER_CONNECTION_KEY = "http2ConnectionKey";
	private final String UNREGISTER_CONNECTION_KEY = "unregisterConnectionKey";
	private final String CHECK_PASS_CONNECTION_KEY = "checkPassConnectionKey";
	private final String CHECK_FAIL_CONNECTION_KEY = "checkFailConnectionKey";


	/**
	 * Construct ConsulClient with all parameters from consul.yml config file. The other two constructors are
	 * just for backward compatibility.
	 */
	public ConsulClientImpl() {
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
			ConsulConnection consulConnection = getConnection(CHECK_PASS_CONNECTION_KEY);
			AtomicReference<ClientResponse> reference = consulConnection.send(Methods.PUT, path, token, null);
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
			ConsulConnection consulConnection = getConnection(CHECK_FAIL_CONNECTION_KEY);
			AtomicReference<ClientResponse> reference = consulConnection.send(Methods.PUT, path, token, null);
			int statusCode = reference.get().getResponseCode();
			if(statusCode >= UNUSUAL_STATUS_CODE){
				logger.error("Failed to checkFail on Consul: {} : {}", statusCode, reference.get().getAttachment(Http2Client.RESPONSE_BODY));
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
			ConsulConnection consulConnection = getConnection(REGISTER_CONNECTION_KEY);
			AtomicReference<ClientResponse> reference = consulConnection.send(Methods.PUT, path, token, json);
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
		String path = "/v1/agent/service/deregister/" + serviceId;
		try {
			ConsulConnection connection = getConnection(UNREGISTER_CONNECTION_KEY);
            final AtomicReference<ClientResponse> reference = connection.send(Methods.PUT, path, token, null);
            int statusCode = reference.get().getResponseCode();
            if(statusCode >= UNUSUAL_STATUS_CODE){
                logger.error("Failed to unregister on Consul, body = {}", reference.get().getAttachment(Http2Client.RESPONSE_BODY));
            }
		} catch (Exception e) {
			logger.error("Failed to unregister on Consul, Exception:", e);
		}
	}

	/**
	 * to lookup health services based on serviceName,
	 * if lastConsulIndex == 0, will get result right away.
	 * if lastConsulIndex != 0, will establish a long query with consul with {@link #wait} seconds.
	 * @param serviceName service name
	 * @param tag tag that is used for filtering
	 * @param lastConsulIndex last consul index
	 * @param token consul token for security
	 * @return null if serviceName is blank
	 */
	@Override
	public ConsulResponse<List<ConsulService>> lookupHealthService(String serviceName, String tag, long lastConsulIndex, String token) {

		ConsulResponse<List<ConsulService>> newResponse = null;

		if(StringUtils.isBlank(serviceName)) {
			return null;
		}

		ConsulConnection connection = getConnection(serviceName);

		String path = "/v1/health/service/" + serviceName + "?passing&wait="+wait+"&index=" + lastConsulIndex;
		if(tag != null) {
			path = path + "&tag=" + tag;
		}
		logger.debug("path = {}", path);
		try {
			AtomicReference<ClientResponse> reference  = connection.send(Methods.GET, path, token, null);
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

	/**
	 * if http2 is enabled, try to establish connect and return the class level {@link #http2Connection}
	 * if http2 is disabled, try to get from the {@link #connectionPool}
	 * @param cacheKey @required cacheKey as the key for caching connection, if not specify cacheKey, will cause NullPointerException
	 * @return ClientConnection
	 */
	private ConsulConnection getConnection(String cacheKey) {
		//the case when enable http2 support use the class level ConsulConnection
        // will use http/2 connection only if tls is enabled as Consul only support HTTP/2 with TLS.
		if(config.isEnableHttp2() && config.getConsulUrl().toLowerCase().startsWith("https")) {
			return this.http2Connection;
		} else {
			ConsulConnection cachedConsulConnection = connectionPool.get(cacheKey);
			if(cachedConsulConnection == null) {
				//init and cache an empty ConsulConnection
				cachedConsulConnection = new ConsulConnection();
				connectionPool.put(cacheKey, cachedConsulConnection);
			}
			return cachedConsulConnection;
		}
	}

	/**
	 * wrapper of ClientConnection, added request counts for each connection
	 */
	private class ConsulConnection {
		private ClientConnection connection;
		private AtomicInteger reqCounter;

		public ConsulConnection() {
			reqCounter = new AtomicInteger(0);
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

		/**
		 * send to consul, init or reconnect if necessary
		 * @param method http method to use
		 * @param path path to send to consul
		 * @param token token to put in header
		 * @param json request body to send
		 * @return AtomicReference<ClientResponse> response
		 */
		 AtomicReference<ClientResponse> send (HttpString method, String path, String token, String json) throws InterruptedException {
			final CountDownLatch latch = new CountDownLatch(1);
			final AtomicReference<ClientResponse> reference = new AtomicReference<>();

			if (needsToCreateConnection()) {
				this.connection = createConnection();
			}

			ClientRequest request = new ClientRequest().setMethod(method).setPath(path);
			request.getRequestHeaders().put(Headers.HOST, "localhost");
			if (token != null) request.getRequestHeaders().put(HttpStringConstants.CONSUL_TOKEN, token);
			logger.debug("The request sent to consul: {} = request header: {}, request body is empty", uri.toString(), request.toString());
			if(StringUtils.isBlank(json)) {
				connection.sendRequest(request, client.createClientCallback(reference, latch));
			} else {
                request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
				connection.sendRequest(request, client.createClientCallback(reference, latch, json));
			}

			latch.await();
			reqCounter.getAndIncrement();
			logger.debug("The response got from consul: {} = {}", uri.toString(), reference.get().toString());
			return reference;
		}

		boolean needsToCreateConnection() {
			return connection == null || !connection.isOpen() || reqCounter.get() >= maxReqPerConn;
		}

		ClientConnection createConnection() {
			logger.debug("connection is closed with counter {}, reconnecting...", reqCounter);
			ClientConnection newConnection = null;
			try {
				newConnection = client.connect(uri, Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, optionMap).get();
			} catch (IOException e) {
				logger.error("cannot create connection to consul {} due to: {}", uri, e.getMessage());
			}
			//whenever init/reconnect, reset request to 0
			reqCounter.set(0);
			return newConnection;
		}
	}
}
