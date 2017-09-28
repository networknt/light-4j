package com.networknt.handler.util;

/**
 * A utility interface with default implementation for extracting headers from the request.
 *
 * @author Bill O'Neil (https://github.com/billoneil)
 * @author Sachin Walia (https://github.com/sachinwalia2k8)
 *
 */

import io.undertow.attribute.RequestHeaderAttribute;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

import java.util.Optional;

public interface Headers {

	default Optional<String> getHeader(HttpServerExchange exchange, HttpString header) {
		RequestHeaderAttribute reqHeader = new RequestHeaderAttribute(header);
		return Optional.ofNullable(reqHeader.readAttribute(exchange));
	}

	default Optional<String> getHeader(HttpServerExchange exchange, String header) {
		RequestHeaderAttribute reqHeader = new RequestHeaderAttribute(new HttpString(header));
		return Optional.ofNullable(reqHeader.readAttribute(exchange));
	}
}