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

package com.networknt.config;

import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** Shared Jackson 2.22.3 feature profile for the framework JSON mappers. */
public final class Jackson2Defaults {
    private Jackson2Defaults() { }

    // Measured on Jackson 2.22.3. Enum feature snapshots do not capture these.
    private static final tools.jackson.core.StreamReadConstraints READ_CONSTRAINTS =
            tools.jackson.core.StreamReadConstraints.builder()
                    .maxNestingDepth(1000).maxDocumentLength(-1L).maxTokenCount(-1L)
                    .maxNumberLength(1000).maxStringLength(20_000_000).maxNameLength(50_000).build();
    private static final tools.jackson.core.StreamWriteConstraints WRITE_CONSTRAINTS =
            tools.jackson.core.StreamWriteConstraints.builder().maxNestingDepth(1000).build();
    private static final JsonMapper MAPPER = builder().build();

    /** Returns the shared immutable mapper with the measured Jackson 2 default policy. */
    public static JsonMapper mapper() {
        return MAPPER;
    }

    /** Creates a builder for a private mapper, retaining the complete measured profile. */
    public static JsonMapper.Builder builder() {
        var factory = tools.jackson.core.json.JsonFactory.builder().configureForJackson2()
                .streamReadConstraints(READ_CONSTRAINTS).streamWriteConstraints(WRITE_CONSTRAINTS).build();
        // configureForJackson2 is documented as incomplete; retain the remaining
        // common-feature differences measured against Jackson 2.22.3 explicitly.
        return JsonMapper.builder(factory).configureForJackson2()
                .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                .disable(MapperFeature.ALLOW_VOID_VALUED_PROPERTIES)
                .enable(SerializationFeature.FAIL_ON_ORDER_MAP_BY_INCOMPARABLE_KEY)
                .disable(StreamReadFeature.USE_FAST_DOUBLE_PARSER, StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER);
    }
}
