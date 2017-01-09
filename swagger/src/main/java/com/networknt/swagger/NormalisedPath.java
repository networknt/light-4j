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

package com.networknt.swagger;

import java.util.List;

/**
 * A normalised representation of an API path.
 * <p>
 * Normalised paths are devoid of path prefixes and contain a normalised starting/ending
 * slash to make comparisons easier.
 */
public interface NormalisedPath {

    /**
     * @return The path parts from the normalised path
     */
    List<String> parts();

    /**
     * @return The path part at the given index
     * @param index position of part
     * @throws IndexOutOfBoundsException if the provided index is not a valid index
     */
    String part(int index);

    /**
     * @return Whether the path part at the given index is a path param (e.g. "/my/{param}/")
     * @param index position of part
     * @throws IndexOutOfBoundsException if the provided index is not a valid index
     */
    boolean isParam(int index);

    /**
     * @return The parameter name of the path part at the given index, or <code>null</code>
     * @param index position of part
     * @throws IndexOutOfBoundsException if the provided index is not a valid index
     */
    String paramName(int index);

    /**
     * @return The original, un-normalised path string
     */
    String original();

    /**
     * @return The normalised path string, with prefixes removed and a standard treatment for leading/trailing slashed.
     */
    String normalised();
}
