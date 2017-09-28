package com.networknt.handler.util;

/**
 * A interface for rendering content with appropriate mime settings.
 *
 * @author Bill O'Neil (https://github.com/billoneil)
 * @author Sachin Walia (https://github.com/sachinwalia2k8)
 *
 */

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.nio.ByteBuffer;

public interface ContentTypeSenders {

	default void sendJson(HttpServerExchange exchange, String json) {
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
		exchange.getResponseSender().send(json);
	}

	default void sendJson(HttpServerExchange exchange, byte[] bytes) {
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
		exchange.getResponseSender().send(ByteBuffer.wrap(bytes));
	}

	default void sendXml(HttpServerExchange exchange, String xml) {
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/xml");
		exchange.getResponseSender().send(xml);
	}

	default void sendHtml(HttpServerExchange exchange, String html) {
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
		exchange.getResponseSender().send(html);
	}

	default void sendText(HttpServerExchange exchange, String text) {
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
		exchange.getResponseSender().send(text);
	}

	default void sendFile(HttpServerExchange exchange, String fileName, String content) {
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/octet-stream");
		exchange.getResponseHeaders().put(Headers.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"");
		exchange.getResponseSender().send(content);
	}

	default void sendFile(HttpServerExchange exchange, String fileName, byte[] bytes) {
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/octet-stream");
		exchange.getResponseHeaders().put(Headers.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"");
		exchange.getResponseSender().send(ByteBuffer.wrap(bytes));
	}
}