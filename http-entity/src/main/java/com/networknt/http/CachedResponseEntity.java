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
 * Extension of {@link ResponseEntity} that adds when the ResponseEntity was cached as epoch millisecond timestamp.
 *
 * @author Diogo Fekete
 * @see #getStatusCode()
 */
public class CachedResponseEntity<T> extends ResponseEntity<T> {

	public Long timestamp = 0L;


	/**
	 * Create a {@code ResponseEntity} with a status code only.
	 * @param status the status code
	 */
	public CachedResponseEntity(HttpStatus status) {
		super(null, null, status);
		this.timestamp = System.currentTimeMillis();
	}

	/**
	 * Create a {@code ResponseEntity} with a body and status code.
	 * @param body the entity body
	 * @param status the status code
	 */
	public CachedResponseEntity(T body, HttpStatus status) {
		super(body, null, status);
		this.timestamp = System.currentTimeMillis();
	}

	/**
	 * Create a {@code ResponseEntity} with headers and a status code.
	 * @param headers the entity headers
	 * @param status the status code
	 */
	public CachedResponseEntity(HeaderMap headers, HttpStatus status) {
		super(null, headers, status);
		this.timestamp = System.currentTimeMillis();
	}

	/**
	 * Create a {@code ResponseEntity} with a body, headers, and a status code.
	 * @param body the entity body
	 * @param headers the entity headers
	 * @param status the status code
	 */
	public CachedResponseEntity(T body, HeaderMap headers, HttpStatus status) {
		super(body, headers, status);
		this.timestamp = System.currentTimeMillis();
	}

	/**
	 * Create a {@code ResponseEntity} with a body, headers, and a raw status code.
	 * @param body the entity body
	 * @param headers the entity headers
	 * @param rawStatus the status code value
	 * @since 5.3.2
	 */
	public CachedResponseEntity(T body, HeaderMap headers, int rawStatus) {
		super(body, headers, HttpStatus.valueOf(rawStatus));
		this.timestamp = System.currentTimeMillis();
	}

	/**
	 * Create a {@code ResponseEntity} with a body, headers, and a status code.
	 * @param body the entity body
	 * @param headers the entity headers
	 * @param status the status code
	 * @param timestamp epoch in milliseconds when the response was cached
	 */
	public CachedResponseEntity(T body, HeaderMap headers, HttpStatus status, Long timestamp) {
		super(body, headers, status);
		this.timestamp = timestamp;
	}

	/**
	 * Create a {@code ResponseEntity} with a body, headers, and a status code.
	 * @param body the entity body
	 * @param headers the entity headers
	 * @param rawStatus the status code integer
	 * @param timestamp epoch in milliseconds when the response was cached
	 */
	public CachedResponseEntity(T body, HeaderMap headers, int rawStatus, Long timestamp) {
		super(body, headers, rawStatus);
		this.timestamp = timestamp;
	}


	/**
	 * Return the timestamp of the cached response.
	 * @return the timestamp in milliseconds
	 */
	public Long getTimestamp() {
		return this.timestamp;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!super.equals(other)) {
			return false;
		}
		CachedResponseEntity<?> otherEntity = (CachedResponseEntity<?>) other;
		return ObjectUtils.nullSafeEquals(this.timestamp, otherEntity.timestamp);
	}

	@Override
	public int hashCode() {
		return (29 * super.hashCode() + ObjectUtils.nullSafeHashCode(this.timestamp));
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("<");
		builder.append(this.timestamp.toString());
		builder.append(',');
		HttpStatus status = getStatusCode();
		if (status != null) {
			builder.append(status.toString());
			builder.append(',');
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

}
