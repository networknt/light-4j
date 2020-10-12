package com.networknt.acme.client.http01;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class HttpChallengeResponder {
	private static final String ACME_CHALLENGE_PATH = "/.well-known/acme-challenge/";
	private static final int PORT = 80;
	static final Logger logger = LoggerFactory.getLogger(HttpChallengeResponder.class);
	private String challengeResponse;
	private HttpServer server;

	public HttpChallengeResponder(String content) throws IOException {
		logger.info(content);
		challengeResponse = content;
		// TODO: handle java.net.SocketException, port 80 can only be opened by root
		server = HttpServer.create(new InetSocketAddress(PORT), 0);
		server.createContext(ACME_CHALLENGE_PATH, new MyHandler());
		server.setExecutor(null);
	}

	public void start() {
		server.start();
	}

	public void stop() {
		server.stop(0);
	}

	class MyHandler implements HttpHandler {
		public void handle(HttpExchange t) throws IOException {
			t.sendResponseHeaders(200, challengeResponse.length());
			OutputStream os = t.getResponseBody();
			os.write(challengeResponse.getBytes());
			os.close();
		}
	}
}
