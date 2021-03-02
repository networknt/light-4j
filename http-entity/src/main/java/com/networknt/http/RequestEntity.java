/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.http;

import com.networknt.utility.ObjectUtils;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.Deque;
import java.util.Map;

/**
 * Extension of {@link HttpEntity} that also exposes the HTTP method and the
 * target URL. For use in the {@code RestTemplate} to prepare requests with
 * and in {@code @Controller} methods to represent request input.
 *
 * <p>Example use with the {@code RestTemplate}:
 * <pre class="code">
 * MyRequest body = ...
 * RequestEntity&lt;MyRequest&gt; request = RequestEntity
 *     .post(&quot;https://example.com/{foo}&quot;, &quot;bar&quot;)
 *     .accept(MediaType.APPLICATION_JSON)
 *     .body(body);
 * ResponseEntity&lt;MyResponse&gt; response = template.exchange(request, MyResponse.class);
 * </pre>
 *
 * <p>Example use in an {@code @Controller}:
 * <pre class="code">
 * &#64;RequestMapping("/handle")
 * public void handle(RequestEntity&lt;String&gt; request) {
 *   HttpMethod method = request.getMethod();
 *   URI url = request.getUrl();
 *   String body = request.getBody();
 * }
 * </pre>
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Parviz Rozikov
 * @since 4.1
 * @param <T> the body type
 * @see #getMethod()
 * @see #getUrl()
 * @see ResponseEntity
 */
public class RequestEntity<T> extends HttpEntity<T> {

	private final HttpMethod method;

	private final URI url;

	private final Type type;

	private Map<String, Deque<String>> queryParameters;

	private Map<String, Deque<String>> pathParameters;

	/**
	 * Constructor with method and URL but without body nor headers.
	 * @param method the method
	 * @param url the URL
	 */
	public RequestEntity(HttpMethod method, URI url) {
		this(null, null, method, url, null, null, null);
	}

	/**
	 * Constructor with method, URL and body but without headers.
	 * @param body the body
	 * @param method the method
	 * @param url the URL
	 */
	public RequestEntity(T body, HttpMethod method, URI url) {
		this(body, null, method, url, null, null, null);
	}

	/**
	 * Constructor with method, URL, body and type but without headers.
	 * @param body the body
	 * @param method the method
	 * @param url the URL
	 * @param type the type used for generic type resolution
	 * @since 4.3
	 */
	public RequestEntity(T body, HttpMethod method, URI url, Type type) {
		this(body, null, method, url, type, null, null);
	}

	/**
	 * Constructor with method, URL and headers but without body.
	 * @param headers the headers
	 * @param method the method
	 * @param url the URL
	 */
	public RequestEntity(HeaderMap headers, HttpMethod method, URI url) {
		this(null, headers, method, url, null, null, null);
	}

	/**
	 * Constructor with method, URL, headers and body.
	 * @param body the body
	 * @param headers the headers
	 * @param method the method
	 * @param url the URL
	 */
	public RequestEntity(T body, HeaderMap headers,
			HttpMethod method, URI url) {

		this(body, headers, method, url, null, null, null);
	}

	/**
	 * Constructor with method, URL, headers, body and type.
	 * @param body the body
	 * @param headers the headers
	 * @param method the method
	 * @param url the URL
	 * @param type the type used for generic type resolution
	 * @param queryParameters the queryParameters
	 * @param pathParameters the pathParameters
	 * @since 4.3
	 */
	public RequestEntity(T body, HeaderMap headers,
			HttpMethod method, URI url, Type type,
            Map<String, Deque<String>> queryParameters,
        	Map<String, Deque<String>> pathParameters) {
		super(body, headers);
		this.method = method;
		this.url = url;
		this.type = type;
		this.queryParameters = queryParameters;
		this.pathParameters = pathParameters;
	}


	/**
	 * Return the HTTP method of the request.
	 * @return the HTTP method as an {@code HttpMethod} enum value
	 */
	public HttpMethod getMethod() {
		return this.method;
	}

	/**
	 * Return the URL of the request.
	 * @return URI
	 */
	public URI getUrl() {
		if (this.url == null) {
			throw new UnsupportedOperationException();
		}
		return this.url;
	}


	/**
	 * Return the type of the request's body.
	 * @return the request's body type, or {@code null} if not known
	 * @since 4.3
	 */
	public Type getType() {
		if (this.type == null) {
			T body = getBody();
			if (body != null) {
				return body.getClass();
			}
		}
		return this.type;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!super.equals(other)) {
			return false;
		}
		RequestEntity<?> otherEntity = (RequestEntity<?>) other;
		return (ObjectUtils.nullSafeEquals(getMethod(), otherEntity.getMethod()) &&
				ObjectUtils.nullSafeEquals(getUrl(), otherEntity.getUrl()));
	}

	@Override
	public int hashCode() {
		int hashCode = super.hashCode();
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.method);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getUrl());
		return hashCode;
	}

	@Override
	public String toString() {
		return format(getMethod(), getUrl().toString(), getBody(), getHeaders());
	}

	static <T> String format(HttpMethod httpMethod, String url, T body, HeaderMap headers) {
		StringBuilder builder = new StringBuilder("<");
		builder.append(httpMethod);
		builder.append(' ');
		builder.append(url);
		builder.append(',');
		if (body != null) {
			builder.append(body);
			builder.append(',');
		}
		builder.append(headers);
		builder.append('>');
		return builder.toString();
	}


	// Static builder methods

	/**
	 * Create a builder with the given method and url.
	 * @param method the HTTP method (GET, POST, etc)
	 * @param url the URL
	 * @param headers the headers
	 * @return the created builder
	 */
	public static BodyBuilder method(HttpMethod method, URI url, HeaderMap headers) {
		return new DefaultBodyBuilder(method, url, headers);
	}

	/**
	 * Defines a builder that adds a body to the response entity.
	 */
	public interface BodyBuilder {

		/**
		 * Set the length of the body in bytes, as specified by the
		 * {@code Content-Length} header.
		 * @param contentLength the content length
		 * @return this builder
		 */
		BodyBuilder contentLength(long contentLength);

		/**
		 * Set the {@linkplain MediaType media type} of the body, as specified
		 * by the {@code Content-Type} header.
		 * @param contentType the content type
		 * @return this builder
		 */
		BodyBuilder contentType(MediaType contentType);

		/**
		 * Set the body of the request entity and build the RequestEntity.
		 * @param <T> the type of the body
		 * @param body the body of the request entity
		 * @return the built request entity
		 */
		<T> RequestEntity<T> body(T body);

		/**
		 * Set the body and type of the request entity and build the RequestEntity.
		 * @param <T> the type of the body
		 * @param body the body of the request entity
		 * @param type the type of the body, useful for generic type resolution
		 * @return the built request entity
		 * @since 4.3
		 */
		<T> RequestEntity<T> body(T body, Type type);
	}


	private static class DefaultBodyBuilder implements BodyBuilder {

		private final HttpMethod method;

		private final HeaderMap headers;

		private final URI uri;


		DefaultBodyBuilder(HttpMethod method, URI url, HeaderMap headers) {
			this.method = method;
			this.uri = url;
			this.headers = headers;
		}

		@Override
		public BodyBuilder contentLength(long contentLength) {
			this.headers.put(Headers.CONTENT_LENGTH, contentLength);
			return this;
		}

		@Override
		public BodyBuilder contentType(MediaType contentType) {
			if(contentType != null) {
				this.headers.put(Headers.CONTENT_TYPE, contentType.toString());
			} else {
				this.headers.remove(Headers.CONTENT_TYPE);
			}
			return this;
		}

		@Override
		public <T> RequestEntity<T> body(T body) {
			return buildInternal(body, null);
		}

		@Override
		public <T> RequestEntity<T> body(T body, Type type) {
			return buildInternal(body, type);
		}

		private <T> RequestEntity<T>  buildInternal(T body, Type type) {
			return new RequestEntity<>(body, this.headers, this.method, this.uri, type, null, null);
		}
	}



	/**
	 * get query parameters
	 *
	 * @return LinkedMultiValueMap
	 */
	public Map<String, Deque<String>> getQueryParameters() {
		return queryParameters;
	}

	/**
	 *  set query parameters
	 * @param queryParameters query parameters
	 */
	public void setQueryParameters(Map<String, Deque<String>> queryParameters) {
		this.queryParameters = queryParameters;
	}

	/**
	 * get path parameters
	 * @return LinkedMultiValueMap
	 */
	public Map<String, Deque<String>> getPathParameters() {
		return pathParameters;
	}

	/**
	 * set path parameters
	 * @param pathParameters path parameters
	 */
	public void setPathParameters(Map<String, Deque<String>> pathParameters) {
		this.pathParameters = pathParameters;
	}
}
