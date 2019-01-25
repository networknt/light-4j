/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.hc.core5.util.copied;

import java.util.Collection;

import org.apache.hc.core5.http.copied.EntityDetails;

public class Args {

    public static void check(final boolean expression, final String message) {
        if (!expression) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void check(final boolean expression, final String message, final Object... args) {
        if (!expression) {
            throw new IllegalArgumentException(String.format(message, args));
        }
    }

    public static void check(final boolean expression, final String message, final Object arg) {
        if (!expression) {
            throw new IllegalArgumentException(String.format(message, arg));
        }
    }

    public static long checkContentLength(final EntityDetails entityDetails) {
        // -1 is a special value
        // 0 is allowed as well
        return checkRange(entityDetails.getContentLength(), -1, Integer.MAX_VALUE,
                        "HTTP entity too large to be buffered in memory)");
    }

    public static int checkRange(final int value, final int lowInclusive, final int highInclusive,
                    final String message) {
        if (value < lowInclusive || value > highInclusive) {
            throw illegalArgumentException("%s: %,d is out of range [%,d, %,d]", message, Integer.valueOf(value),
                            Integer.valueOf(lowInclusive), Integer.valueOf(highInclusive));
        }
        return value;
    }

    public static long checkRange(final long value, final long lowInclusive, final long highInclusive,
                    final String message) {
        if (value < lowInclusive || value > highInclusive) {
            throw illegalArgumentException("%s: %,d is out of range [%,d, %,d]", message, Long.valueOf(value),
                            Long.valueOf(lowInclusive), Long.valueOf(highInclusive));
        }
        return value;
    }

    public static <T extends CharSequence> T containsNoBlanks(final T argument, final String name) {
        if (argument == null) {
            throw illegalArgumentExceptionNotNull(name);
        }
        if (argument.length() == 0) {
            throw illegalArgumentExceptionNotEmpty(name);
        }
        if (TextUtils.containsBlanks(argument)) {
            throw new IllegalArgumentException(name + " must not contain blanks");
        }
        return argument;
    }

    private static IllegalArgumentException illegalArgumentException(final String format, final Object... args) {
        return new IllegalArgumentException(String.format(format, args));
    }

    private static IllegalArgumentException illegalArgumentExceptionNotEmpty(final String name) {
        return new IllegalArgumentException(name + " must not be empty");
    }

    private static IllegalArgumentException illegalArgumentExceptionNotNull(final String name) {
        return new IllegalArgumentException(name + " must not be null");
    }

    public static <T extends CharSequence> T notBlank(final T argument, final String name) {
        if (argument == null) {
            throw illegalArgumentExceptionNotNull(name);
        }
        if (TextUtils.isBlank(argument)) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return argument;
    }

    public static <T extends CharSequence> T notEmpty(final T argument, final String name) {
        if (argument == null) {
            throw illegalArgumentExceptionNotNull(name);
        }
        if (TextUtils.isEmpty(argument)) {
            throw illegalArgumentExceptionNotEmpty(name);
        }
        return argument;
    }

    public static <E, T extends Collection<E>> T notEmpty(final T argument, final String name) {
        if (argument == null) {
            throw illegalArgumentExceptionNotNull(name);
        }
        if (argument.isEmpty()) {
            throw illegalArgumentExceptionNotEmpty(name);
        }
        return argument;
    }

    public static int notNegative(final int n, final String name) {
        if (n < 0) {
            throw illegalArgumentException("%s must not be negative: %,d", name, n);
        }
        return n;
    }

    public static long notNegative(final long n, final String name) {
        if (n < 0) {
            throw illegalArgumentException("%s must not be negative: %,d", name, n);
        }
        return n;
    }

    public static <T> T notNull(final T argument, final String name) {
        if (argument == null) {
            throw illegalArgumentExceptionNotNull(name);
        }
        return argument;
    }

    public static int positive(final int n, final String name) {
        if (n <= 0) {
            throw illegalArgumentException("%s must not be negative or zero: %,d", name, n);
        }
        return n;
    }

    public static long positive(final long n, final String name) {
        if (n <= 0) {
            throw illegalArgumentException("%s must not be negative or zero: %,d", name, n);
        }
        return n;
    }

    private Args() {
        // Do not allow utility class to be instantiated.
    }

}
