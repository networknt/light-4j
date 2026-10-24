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
final class Jackson2Defaults {
    private Jackson2Defaults() { }

    static JsonMapper.Builder builder() {
        // Jackson documents configureForJackson2 as incomplete. These additional
        // common-feature settings come from the measured 2.22.3 baseline.
        return JsonMapper.builderWithJackson2Defaults()
                .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                .disable(MapperFeature.ALLOW_VOID_VALUED_PROPERTIES)
                .enable(SerializationFeature.FAIL_ON_ORDER_MAP_BY_INCOMPARABLE_KEY)
                .disable(StreamReadFeature.USE_FAST_DOUBLE_PARSER, StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER);
    }
}
