package com.networknt.handler.util;

/**
 * A utility interface with default implementations for extracting path variables from the URL.
 *
 * @author Bill O'Neil (https://github.com/billoneil)
 * @author Sachin Walia (https://github.com/sachinwalia2k8)
 *
 */

import io.undertow.server.HttpServerExchange;

import java.util.Deque;
import java.util.Optional;

public interface PathParams {

	default Optional<String> pathParam(HttpServerExchange exchange, String name) {
		/*
		 *  I think there is a bug with path params in routing handler, will revisit.
		 *  Luckily RoutingHandler by default puts all path params into the query params.
		 */
		return Optional.ofNullable(exchange.getQueryParameters().get(name))
			.map(Deque::getFirst);
	}

	default Optional<Long> pathParamAsLong(HttpServerExchange exchange, String name) {
		return pathParam(exchange, name).map(Long::parseLong);
	}

	default Optional<Integer> pathParamAsInteger(HttpServerExchange exchange, String name) {
		return pathParam(exchange, name).map(Integer::parseInt);
	}
}