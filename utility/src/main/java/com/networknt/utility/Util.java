/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.utility;

import org.apache.commons.codec.binary.Base64;

import java.nio.ByteBuffer;
import java.util.UUID;

public class Util {

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

    /**
     * Quote the given string if needed
     *
     * @param value The value to quote (e.g. bob)
     * @return The quoted string (e.g. "bob")
     */
    public static String quote(final String value) {
        if (value == null) {
            return value;
        }
        String result = value;
        if (!result.startsWith("\"")) {
            result = "\"" + result;
        }
        if (!result.endsWith("\"")) {
            result = result + "\"";
        }
        return result;
    }

}
