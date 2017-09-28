package com.networknt.handler.util;

/**
 * A utility interface with default implementations for extracting query parameters from the URL.
 *
 * @author Bill O'Neil (https://github.com/billoneil)
 * @author Sachin Walia (https://github.com/sachinwalia2k8)
 *
 */

import io.undertow.server.HttpServerExchange;

import java.util.Deque;
import java.util.Optional;

public interface QueryParams {

	default Optional<String> queryParam(HttpServerExchange exchange, String name) {
		return Optional.ofNullable(exchange.getQueryParameters().get(name))
			.map(Deque::getFirst);
	}

	default Optional<Long> queryParamAsLong(HttpServerExchange exchange, String name) {
		return queryParam(exchange, name).map(Long::parseLong);
	}

	default Optional<Integer> queryParamAsInteger(HttpServerExchange exchange, String name) {
		return queryParam(exchange, name).map(Integer::parseInt);
	}

	default Optional<Boolean> queryParamAsBoolean(HttpServerExchange exchange, String name) {
		return queryParam(exchange, name).map(Boolean::parseBoolean);
	}
}