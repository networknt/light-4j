package com.networknt.utility;

import org.apache.commons.codec.binary.Base64;

import java.nio.ByteBuffer;
import java.util.UUID;

public class ApiUtil {

	/**
	 * Generate UUID across the entire app and it is used for correlationId.
	 *
	 * @return String correlationId
	 */
	public static String getUUID() {
		UUID id = UUID.randomUUID();
		ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
		bb.putLong(id.getMostSignificantBits());
		bb.putLong(id.getLeastSignificantBits());
		return Base64.encodeBase64URLSafeString(bb.array());
	}
	
}
