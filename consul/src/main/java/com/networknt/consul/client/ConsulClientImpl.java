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
import com.networknt.consul.*;
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
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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

	private OptionMap optionMap;
	private URI uri;
	private String wait = "600s";
	private String timeoutBuffer = "5s";

	/**
	 * Construct ConsulClient with all parameters from consul.yml config file. The other two constructors are
	 * just for backward compatibility.
	 */
	public ConsulClientImpl() {
		String consulUrl = config.getConsulUrl().toLowerCase();
		optionMap =  isHttp2() ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true) : OptionMap.EMPTY;
		logger.debug("Consul URL = {}", consulUrl);
		if(config.getWait() != null && config.getWait().length() > 2) wait = config.getWait();
		logger.debug("wait = {}", wait);
		if(config.getTimeoutBuffer() != null) timeoutBuffer = config.getTimeoutBuffer();
		logger.debug("timeoutBuffer = {}", timeoutBuffer);
		try {
			uri = new URI(consulUrl);
		} catch (URISyntaxException e) {
			logger.error("Consul URL generated invalid URI! Consul URL: " + consulUrl, e);
			throw new RuntimeException("Invalid URI " + consulUrl, e);
		}
	}

	@Override
	public void checkPass(String serviceId, String token) {
		logger.trace("checkPass serviceId = {}", serviceId);
		String path = "/v1/agent/check/pass/" + "check-" + serviceId;
		ClientConnection connection = null;
		try {
			connection = client.safeBorrowConnection(
					config.getConnectionTimeout(), uri, Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, optionMap);

			AtomicReference<ClientResponse> reference = send(connection, Methods.PUT, path, token, null);
			int statusCode = reference.get().getResponseCode();
			if(statusCode >= UNUSUAL_STATUS_CODE){
				logger.error("Failed to checkPass on Consul: {} : {}", statusCode, reference.get().getAttachment(Http2Client.RESPONSE_BODY));
				throw new Exception("Failed to checkPass on Consul: " + statusCode + ":" + reference.get().getAttachment(Http2Client.RESPONSE_BODY));
			}
		} catch (Exception e) {
			logger.error("CheckPass request exception", e);
		} finally {
			client.returnConnection(connection);
		}
	}

	@Override
	public void checkFail(String serviceId, String token) {
		logger.trace("checkFail serviceId = {}", serviceId);
		String path = "/v1/agent/check/fail/" + "check-" + serviceId;
		ClientConnection connection = null;
		try {
			connection = client.safeBorrowConnection(
					config.getConnectionTimeout(), uri, Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, optionMap);

			AtomicReference<ClientResponse> reference = send(connection, Methods.PUT, path, token, null);
			int statusCode = reference.get().getResponseCode();
			if(statusCode >= UNUSUAL_STATUS_CODE){
				logger.error("Failed to checkFail on Consul: {} : {}", statusCode, reference.get().getAttachment(Http2Client.RESPONSE_BODY));
			}
		} catch (Exception e) {
			logger.error("CheckFail request exception", e);
		} finally {
			client.returnConnection(connection);
		}
	}

	@Override
	public void registerService(ConsulService service, String token) {
		String json = service.toString();
		String path = "/v1/agent/service/register";
		ClientConnection connection = null;
		try {
			connection = client.safeBorrowConnection(
					config.getConnectionTimeout(), uri, Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, optionMap);

			AtomicReference<ClientResponse> reference = send(connection, Methods.PUT, path, token, json);
			int statusCode = reference.get().getResponseCode();
			if(statusCode >= UNUSUAL_STATUS_CODE){
				throw new Exception("Failed to register on Consul: " + statusCode);
			}
		} catch (Exception e) {
			logger.error("Failed to register on Consul, Exception:", e);
			throw new RuntimeException(e.getMessage());
		} finally {
			client.returnConnection(connection);
		}
	}

	@Override
	public void unregisterService(String serviceId, String token) {
		String path = "/v1/agent/service/deregister/" + serviceId;
		ClientConnection connection = null;
		try {
			connection = client.safeBorrowConnection(
					config.getConnectionTimeout(), uri, Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, optionMap);

	        final AtomicReference<ClientResponse> reference = send(connection, Methods.PUT, path, token, null);
            int statusCode = reference.get().getResponseCode();
            if(statusCode >= UNUSUAL_STATUS_CODE){
                logger.error("Failed to unregister on Consul, body = {}", reference.get().getAttachment(Http2Client.RESPONSE_BODY));
            }
		} catch (Exception e) {
			logger.error("Failed to unregister on Consul, Exception:", e);
		} finally {
			client.returnConnection(connection);
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
	 * @return null if serviceName is blank	// TODO: We only want null returned if there is an error connecting to Consul
	 */
	@Override
	public ConsulResponse<List<ConsulService>> lookupHealthService(String serviceName, String tag, long lastConsulIndex, String token) {

		ConsulResponse<List<ConsulService>> newResponse = null;

		// TODO: Remove this if possible - We only want null returned if there is an error connecting to Consul
		// Calls to lookupHealthService with a blank serviceName should now be impossible due to updates
		// in LightCluster (commit 6e5c29b2) and ConsulRegistry (commit d2957a8d)
		if(StringUtils.isBlank(serviceName)) {
			return null;
		}

		ClientConnection connection = null;
		String path = "/v1/health/service/" + serviceName + "?passing&wait="+wait+"&index=" + lastConsulIndex;
		if(tag != null) {
			path = path + "&tag=" + tag;
		}
		logger.trace("Consul health service path = {}", path);

		try {
			logger.debug("Getting connection from pool with {}", uri);
			// this will throw a Runtime Exception if creation of Consul connection fails
			connection = client.safeBorrowConnection(
					config.getConnectionTimeout(), uri, Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, optionMap);

			logger.info("Got connection: {} from pool and send request to {}", connection, path);
			// TODO: Ask NetworkNT why an AtomicReference is used here
			// TODO: Pass timeout value into send() methods since different methods require different timeouts
			AtomicReference<ClientResponse> reference = send(connection, Methods.GET, path, token, null);

			// Check that reference.get() is not null
			if(reference.get() == null)
				throw new ConsulConnectionException("Connection to Consul failed - Received null response");

			int statusCode = reference.get().getResponseCode();
			logger.info("Got Consul Query status code: {}", statusCode);

			if(statusCode >= UNUSUAL_STATUS_CODE){
				throw new Exception("Consul Query returned an error: " + statusCode);
			} else {
				String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
				logger.debug("Got Consul Query response body: {}", body);

				// convert the service instances of serviceName to Java objects
				List<Map<String, Object>> services =
						Config.getInstance().getMapper().readValue(body, new TypeReference<List<Map<String, Object>>>(){});
				List<ConsulService> consulServices = new ArrayList<>(services.size());

				for (Map<String, Object> service : services) {
					ConsulService newService = convertToConsulService((Map<String,Object>)service.get("Service"));
					consulServices.add(newService);
				}

				// Previously, consulServices.isEmpty()==true causes this method to return null on a successful Consul
				// request, when no IPs are returned
				//if (!consulServices.isEmpty()) {
				newResponse = new ConsulResponse<>();
				newResponse.setValue(consulServices);
				newResponse.setConsulIndex(Long.parseLong(reference.get().getResponseHeaders().getFirst("X-Consul-Index")));
				newResponse.setConsulLastContact(Long.parseLong(reference.get().getResponseHeaders().getFirst("X-Consul-Lastcontact")));
				newResponse.setConsulKnownLeader(Boolean.parseBoolean(reference.get().getResponseHeaders().getFirst("X-Consul-Knownleader")));
				//}
			}
		} catch (ConsulConnectionException e) {
			// This should only return null if Consul connection fails
			logger.error("Exception:", e);

			logger.debug("Terminating connection to Consul");
			if(connection != null && connection.isOpen())
				IoUtils.safeClose(connection);
			return null;

		} catch(Exception e) {
			// This should only return null if Consul connection fails
			logger.error("Exception:", e);
			return null;

		} finally {
			client.returnConnection(connection);
		}

		return newResponse;
	}

	private static class ConsulConnectionException extends RuntimeException
	{
		public ConsulConnectionException(String message) {
			super(message);
		}
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
	 * send to consul with the passed in connection
	 * @param connection ClientConnection
	 * @param method http method to use
	 * @param path path to send to consul
	 * @param token token to put in header
	 * @param json request body to send
	 * @return AtomicReference<ClientResponse> response
	 */
	AtomicReference<ClientResponse> send(ClientConnection connection, HttpString method, String path, String token, String json)
			throws InterruptedException
	{
		// construct request
		ClientRequest request = new ClientRequest().setMethod(method).setPath(path);
		request.getRequestHeaders().put(Headers.HOST, "localhost");
		if (token != null)
			request.getRequestHeaders().put(HttpStringConstants.CONSUL_TOKEN, token);
		if (logger.isTraceEnabled())
			logger.trace("The request sent to Consul URI {} - request header: {}, request body is empty", uri.toString(), request.toString());

		// send request
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<ClientResponse> reference = new AtomicReference<>();
		if(StringUtils.isBlank(json)) {
			connection.sendRequest(request, client.createClientCallback(reference, latch));
		} else {
			request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
			connection.sendRequest(request, client.createClientCallback(reference, latch, json));
		}

		// Await response and ensure we do not block if there are network or Consul server issues
		// TODO: Add random jitter to timeout
		// TODO: Have caller specify the timeout, since not all calls should have a getWaitInSecond() length timeout
		int waitInSecond = ConsulUtils.getWaitInSecond(wait);
		int timeoutBufferInSecond = ConsulUtils.getTimeoutBufferInSecond(timeoutBuffer);
		boolean isNotTimeout = latch.await(waitInSecond + timeoutBufferInSecond, TimeUnit.SECONDS);

		if (isNotTimeout) {
			logger.debug("The response from Consul: {} = {}", uri, reference != null ? reference.get() : null);
		} else {
            // If a timeout occurs, it is not known whether Consul is still alive.
			// Close the connection to force reconnect: The next time this connection is borrowed from the pool, a new
			// connection will be created as the one returned is not open.
			if(connection != null && connection.isOpen()) IoUtils.safeClose(connection);
			throw new RuntimeException(
					String.format("The request to Consul timed out after %d + %d seconds to: %s", waitInSecond, timeoutBufferInSecond, uri));
		}
		return reference;
	}

	/**
	 * As the Consul server is built with Go and HTTP/2 is supported by default when HTTPS is used, we need to leverage
	 * the multiplexing of HTTP/2 whenever possible. In the scenario that the user miss the enableHttp2 flag in the
	 * consul.yml config file, we will force the Consul client to use HTTP/2 if the consulUrl is starting with "https".
	 *
 	 * @return true if we want to use HTTP/2 to connect to the Consul.
	 */
	private boolean isHttp2() {
		return config.isEnableHttp2() || config.getConsulUrl().toLowerCase().startsWith("https");
	}
}
