/*
 * Copyright 2002-2021 the original author or authors.
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

/**
 * Extension of {@link HttpEntity} that adds an {@link HttpStatus} status code.
 * Used in {@code RestTemplate} as well as in {@code @Controller} methods.
 *
 * <p>In {@code RestTemplate}, this class is returned by
 * <pre class="code">
 * ResponseEntity&lt;String&gt; entity = template.getForEntity("https://example.com", String.class);
 * String body = entity.getBody();
 * MediaType contentType = entity.getHeaders().getContentType();
 * HttpStatus statusCode = entity.getStatusCode();
 * </pre>
 *
 * <p>This can also be used in Spring MVC as the return value from an
 * {@code @Controller} method:
 * <pre class="code">
 * &#64;RequestMapping("/handle")
 * public ResponseEntity&lt;String&gt; handle() {
 *   URI location = ...;
 *   HttpHeaders responseHeaders = new HttpHeaders();
 *   responseHeaders.setLocation(location);
 *   responseHeaders.set("MyResponseHeader", "MyValue");
 *   return new ResponseEntity&lt;String&gt;("Hello World", responseHeaders, HttpStatus.CREATED);
 * }
 * </pre>
 *
 * Or, by using a builder accessible via static methods:
 * <pre class="code">
 * &#64;RequestMapping("/handle")
 * public ResponseEntity&lt;String&gt; handle() {
 *   URI location = ...;
 *   return ResponseEntity.created(location).header("MyResponseHeader", "MyValue").body("Hello World");
 * }
 * </pre>
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @since 3.0.2
 * @param <T> the body type
 * @see #getStatusCode()
 */
public class ResponseEntity<T> extends HttpEntity<T> {

	private final Object status;


	/**
	 * Create a {@code ResponseEntity} with a status code only.
	 * @param status the status code
	 */
	public ResponseEntity(HttpStatus status) {
		this(null, null, status);
	}

	/**
	 * Create a {@code ResponseEntity} with a body and status code.
	 * @param body the entity body
	 * @param status the status code
	 */
	public ResponseEntity(T body, HttpStatus status) {
		this(body, null, status);
	}

	/**
	 * Create a {@code ResponseEntity} with headers and a status code.
	 * @param headers the entity headers
	 * @param status the status code
	 */
	public ResponseEntity(HeaderMap headers, HttpStatus status) {
		this(null, headers, status);
	}

	/**
	 * Create a {@code ResponseEntity} with a body, headers, and a status code.
	 * @param body the entity body
	 * @param headers the entity headers
	 * @param status the status code
	 */
	public ResponseEntity(T body, HeaderMap headers, HttpStatus status) {
		this(body, headers, (Object) status);
	}

	/**
	 * Create a {@code ResponseEntity} with a body, headers, and a raw status code.
	 * @param body the entity body
	 * @param headers the entity headers
	 * @param rawStatus the status code value
	 * @since 5.3.2
	 */
	public ResponseEntity(T body, HeaderMap headers, int rawStatus) {
		this(body, headers, (Object) rawStatus);
	}

	/**
	 * Private constructor.
	 */
	private ResponseEntity(T body, HeaderMap headers, Object status) {
		super(body, headers);
		this.status = status;
	}


	/**
	 * Return the HTTP status code of the response.
	 * @return the HTTP status as an HttpStatus enum entry
	 */
	public HttpStatus getStatusCode() {
		if (this.status instanceof HttpStatus) {
			return (HttpStatus) this.status;
		}
		else {
			return HttpStatus.valueOf((Integer) this.status);
		}
	}

	/**
	 * Return the HTTP status code of the response.
	 * @return the HTTP status as an int value
	 * @since 4.3
	 */
	public int getStatusCodeValue() {
		if (this.status instanceof HttpStatus) {
			return ((HttpStatus) this.status).value();
		}
		else {
			return (Integer) this.status;
		}
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!super.equals(other)) {
			return false;
		}
		ResponseEntity<?> otherEntity = (ResponseEntity<?>) other;
		return ObjectUtils.nullSafeEquals(this.status, otherEntity.status);
	}

	@Override
	public int hashCode() {
		return (29 * super.hashCode() + ObjectUtils.nullSafeHashCode(this.status));
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("<");
		builder.append(this.status.toString());
		if (this.status instanceof HttpStatus) {
			builder.append(' ');
			builder.append(((HttpStatus) this.status).getReasonPhrase());
		}
		builder.append(',');
		T body = getBody();
		HeaderMap headers = getHeaders();
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
	 * Create a builder with the given status.
	 * @param status the response status
	 * @return the created builder
	 * @since 4.1
	 */
	public static BodyBuilder status(HttpStatus status) {
		return new DefaultBuilder(status);
	}

	/**
	 * Create a builder with the given status.
	 * @param status the response status
	 * @return the created builder
	 * @since 4.1
	 */
	public static BodyBuilder status(int status) {
		return new DefaultBuilder(status);
	}

	/**
	 * Create a builder with the status set to {@linkplain HttpStatus#OK OK}.
	 * @return the created builder
	 * @since 4.1
	 */
	public static BodyBuilder ok() {
		return status(HttpStatus.OK);
	}

	/**
	 * A shortcut for creating a {@code ResponseEntity} with the given body
	 * and the status set to {@linkplain HttpStatus#OK OK}.
	 * @param body the body of the response entity (possibly empty)
	 * @param <T> response entity
	 * @return the created {@code ResponseEntity}
	 * @since 4.1
	 */
	public static <T> ResponseEntity<T> ok(T body) {
		return ok().body(body);
	}

	/**
	 * Create a builder with an {@linkplain HttpStatus#ACCEPTED ACCEPTED} status.
	 * @return the created builder
	 * @since 4.1
	 */
	public static BodyBuilder accepted() {
		return status(HttpStatus.ACCEPTED);
	}


	/**
	 * Create a builder with a {@linkplain HttpStatus#BAD_REQUEST BAD_REQUEST} status.
	 * @return the created builder
	 * @since 4.1
	 */
	public static BodyBuilder badRequest() {
		return status(HttpStatus.BAD_REQUEST);
	}

	/**
	 * Create a builder with an
	 * {@linkplain HttpStatus#UNPROCESSABLE_ENTITY UNPROCESSABLE_ENTITY} status.
	 * @return the created builder
	 * @since 4.1.3
	 */
	public static BodyBuilder unprocessableEntity() {
		return status(HttpStatus.UNPROCESSABLE_ENTITY);
	}

	/**
	 * Defines a builder that adds a body to the response entity.
	 * @since 4.1
	 */
	public interface BodyBuilder {

		/**
		 * Set the {@linkplain MediaType media type} of the body, as specified by the
		 * {@code Content-Type} header.
		 * @param contentType the content type
		 * @return this builder
		 */
		BodyBuilder contentType(MediaType contentType);

		/**
		 * Set the body of the response entity and returns it.
		 * @param <T> the type of the body
		 * @param body the body of the response entity
		 * @return the built response entity
		 */
		<T> ResponseEntity<T> body(T body);
	}


	private static class DefaultBuilder implements BodyBuilder {

		private final Object statusCode;

		private final HeaderMap headers;

		public DefaultBuilder(Object statusCode) {
			this(statusCode, new HeaderMap());
		}

		public DefaultBuilder(Object statusCode, HeaderMap headers) {
			this.statusCode = statusCode;
			this.headers = headers;
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
		public <T> ResponseEntity<T> body(T body) {
			return new ResponseEntity<>(body, this.headers, this.statusCode);
		}
	}

}
