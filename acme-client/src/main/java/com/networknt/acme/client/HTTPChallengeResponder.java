package com.networknt.acme.client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class HTTPChallengeResponder {
	private String challengeResponse;
	private HttpServer server;
	private static final String acmePath = "/.well-known/acme-challenge/";
	private static final int port = 80;
	static final Logger logger = LoggerFactory.getLogger(HTTPChallengeResponder.class);
	public HTTPChallengeResponder(String content) throws IOException {
		logger.info(challengeResponse);
		challengeResponse = content;
		server=HttpServer.create(new InetSocketAddress(port), 0);
		server.createContext(acmePath, new MyHandler());
		server.setExecutor(null); // creates a default executor
		server.start();
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

