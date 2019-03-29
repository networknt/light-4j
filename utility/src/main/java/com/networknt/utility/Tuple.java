/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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

/**
 * @param <T> The type of the first element
 * @param <T2> The type of the second element.
 *
 * @author Nicholas Azar
 */
public class Tuple<T, T2> {
    public final T first;
    public final T2 second;

    public Tuple(T first, T2 second) {
        this.first = first;
        this.second = second;
    }
}
