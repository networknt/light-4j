/*
 * Copyright 2002-2019 the original author or authors.
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
import com.networknt.utility.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

/**
 * Represents an HTTP (byte) range for use with the HTTP {@code "Range"} header.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 4.2
 * @see <a href="https://tools.ietf.org/html/rfc7233">HTTP/1.1: Range Requests</a>
 */
public abstract class HttpRange {

	/** Maximum ranges per request. */
	private static final int MAX_RANGES = 100;

	private static final String BYTE_RANGE_PREFIX = "bytes=";


	/**
	 * Return the start of the range given the total length of a representation.
	 * @param length the length of the representation
	 * @return the start of this range for the representation
	 */
	public abstract long getRangeStart(long length);

	/**
	 * Return the end of the range (inclusive) given the total length of a representation.
	 * @param length the length of the representation
	 * @return the end of the range for the representation
	 */
	public abstract long getRangeEnd(long length);


	/**
	 * Create an {@code HttpRange} from the given position to the end.
	 * @param firstBytePos the first byte position
	 * @return a byte range that ranges from {@code firstPos} till the end
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-2.1">Byte Ranges</a>
	 */
	public static HttpRange createByteRange(long firstBytePos) {
		return new ByteRange(firstBytePos, null);
	}

	/**
	 * Create a {@code HttpRange} from the given fist to last position.
	 * @param firstBytePos the first byte position
	 * @param lastBytePos the last byte position
	 * @return a byte range that ranges from {@code firstPos} till {@code lastPos}
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-2.1">Byte Ranges</a>
	 */
	public static HttpRange createByteRange(long firstBytePos, long lastBytePos) {
		return new ByteRange(firstBytePos, lastBytePos);
	}

	/**
	 * Create an {@code HttpRange} that ranges over the last given number of bytes.
	 * @param suffixLength the number of bytes for the range
	 * @return a byte range that ranges over the last {@code suffixLength} number of bytes
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-2.1">Byte Ranges</a>
	 */
	public static HttpRange createSuffixRange(long suffixLength) {
		return new SuffixByteRange(suffixLength);
	}

	/**
	 * Parse the given, comma-separated string into a list of {@code HttpRange} objects.
	 * <p>This method can be used to parse an {@code Range} header.
	 * @param ranges the string to parse
	 * @return the list of ranges
	 * @throws IllegalArgumentException if the string cannot be parsed
	 * or if the number of ranges is greater than 100
	 */
	public static List<HttpRange> parseRanges(String ranges) {
		if (!StringUtils.hasLength(ranges)) {
			return Collections.emptyList();
		}
		if (!ranges.startsWith(BYTE_RANGE_PREFIX)) {
			throw new IllegalArgumentException("Range '" + ranges + "' does not start with 'bytes='");
		}
		ranges = ranges.substring(BYTE_RANGE_PREFIX.length());

		String[] tokens = StringUtils.tokenizeToStringArray(ranges, ",");
		if (tokens.length > MAX_RANGES) {
			throw new IllegalArgumentException("Too many ranges: " + tokens.length);
		}
		List<HttpRange> result = new ArrayList<>(tokens.length);
		for (String token : tokens) {
			result.add(parseRange(token));
		}
		return result;
	}

	private static HttpRange parseRange(String range) {
		int dashIdx = range.indexOf('-');
		if (dashIdx > 0) {
			long firstPos = Long.parseLong(range.substring(0, dashIdx));
			if (dashIdx < range.length() - 1) {
				Long lastPos = Long.parseLong(range.substring(dashIdx + 1));
				return new ByteRange(firstPos, lastPos);
			}
			else {
				return new ByteRange(firstPos, null);
			}
		}
		else if (dashIdx == 0) {
			long suffixLength = Long.parseLong(range.substring(1));
			return new SuffixByteRange(suffixLength);
		}
		else {
			throw new IllegalArgumentException("Range '" + range + "' does not contain \"-\"");
		}
	}

	/**
	 * Return a string representation of the given list of {@code HttpRange} objects.
	 * <p>This method can be used to for an {@code Range} header.
	 * @param ranges the ranges to create a string of
	 * @return the string representation
	 */
	public static String toString(Collection<HttpRange> ranges) {
		StringJoiner builder = new StringJoiner(", ", BYTE_RANGE_PREFIX, "");
		for (HttpRange range : ranges) {
			builder.add(range.toString());
		}
		return builder.toString();
	}


	/**
	 * Represents an HTTP/1.1 byte range, with a first and optional last position.
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-2.1">Byte Ranges</a>
	 * @see HttpRange#createByteRange(long)
	 * @see HttpRange#createByteRange(long, long)
	 */
	private static class ByteRange extends HttpRange {

		private final long firstPos;

		private final Long lastPos;

		public ByteRange(long firstPos, Long lastPos) {
			assertPositions(firstPos, lastPos);
			this.firstPos = firstPos;
			this.lastPos = lastPos;
		}

		private void assertPositions(long firstBytePos, Long lastBytePos) {
			if (firstBytePos < 0) {
				throw new IllegalArgumentException("Invalid first byte position: " + firstBytePos);
			}
			if (lastBytePos != null && lastBytePos < firstBytePos) {
				throw new IllegalArgumentException("firstBytePosition=" + firstBytePos +
						" should be less then or equal to lastBytePosition=" + lastBytePos);
			}
		}

		@Override
		public long getRangeStart(long length) {
			return this.firstPos;
		}

		@Override
		public long getRangeEnd(long length) {
			if (this.lastPos != null && this.lastPos < length) {
				return this.lastPos;
			}
			else {
				return length - 1;
			}
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof ByteRange)) {
				return false;
			}
			ByteRange otherRange = (ByteRange) other;
			return (this.firstPos == otherRange.firstPos &&
					ObjectUtils.nullSafeEquals(this.lastPos, otherRange.lastPos));
		}

		@Override
		public int hashCode() {
			return (ObjectUtils.nullSafeHashCode(this.firstPos) * 31 +
					ObjectUtils.nullSafeHashCode(this.lastPos));
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(this.firstPos);
			builder.append('-');
			if (this.lastPos != null) {
				builder.append(this.lastPos);
			}
			return builder.toString();
		}
	}


	/**
	 * Represents an HTTP/1.1 suffix byte range, with a number of suffix bytes.
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-2.1">Byte Ranges</a>
	 * @see HttpRange#createSuffixRange(long)
	 */
	private static class SuffixByteRange extends HttpRange {

		private final long suffixLength;

		public SuffixByteRange(long suffixLength) {
			if (suffixLength < 0) {
				throw new IllegalArgumentException("Invalid suffix length: " + suffixLength);
			}
			this.suffixLength = suffixLength;
		}

		@Override
		public long getRangeStart(long length) {
			if (this.suffixLength < length) {
				return length - this.suffixLength;
			}
			else {
				return 0;
			}
		}

		@Override
		public long getRangeEnd(long length) {
			return length - 1;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof SuffixByteRange)) {
				return false;
			}
			SuffixByteRange otherRange = (SuffixByteRange) other;
			return (this.suffixLength == otherRange.suffixLength);
		}

		@Override
		public int hashCode() {
			return Long.hashCode(this.suffixLength);
		}

		@Override
		public String toString() {
			return "-" + this.suffixLength;
		}
	}

}
