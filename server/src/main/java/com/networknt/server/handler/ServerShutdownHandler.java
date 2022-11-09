package com.networknt.server.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.handler.LightHttpHandler;
import com.networknt.server.Server;
import com.networknt.server.ServerConfig;
import com.networknt.server.model.ServerShutdownResponse;

import io.undertow.server.HttpServerExchange;

/**
 * Light-4j handler to shut down and force restart the service deployed to the Kubernetes cluster
 * Or running as a service in Windows Server or Linux.
 *
 */
public class ServerShutdownHandler implements LightHttpHandler {

	private static final Logger logger = LoggerFactory.getLogger(ServerShutdownHandler.class);

	public ServerShutdownHandler() {
		logger.info("ServerShutdownHandler constructed");
	}

	@Override
	public void handleRequest(final HttpServerExchange exchange) throws Exception {
		try {

			ServerConfig serverConfig = (ServerConfig) Config.getInstance()
					.getJsonObjectConfig(ServerConfig.CONFIG_NAME, ServerConfig.class);

			ServerShutdownResponse response = new ServerShutdownResponse();
			response.setTime(System.currentTimeMillis());
			response.setServiceId(serverConfig.getServiceId());
			response.setTag(serverConfig.getEnvironment());

			exchange.getResponseSender().send(JsonMapper.toJson(response));

			logger.info("ServerShutdownHandler - Killing the Server!");
			Server.shutdown();
			System.exit(0);

		} catch (Exception e) {
			logger.info("ServerShutdownHandler - Unable to kill the Server!", e);
		}
	}

}
