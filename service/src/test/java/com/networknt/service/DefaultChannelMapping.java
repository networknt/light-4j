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

package com.networknt.service;

import java.util.HashMap;
import java.util.Map;

public class DefaultChannelMapping implements ChannelMapping {

    private Map<String, String> mappings;

    public static class DefaultChannelMappingBuilder {

        private Map<String, String> mappings = new HashMap<>();

        public DefaultChannelMappingBuilder with(String from, String to) {
            mappings.put(from, to);
            return this;
        }

        public ChannelMapping build() {
            return new DefaultChannelMapping(mappings);
        }
    }
    public static DefaultChannelMappingBuilder builder() {
        return new DefaultChannelMappingBuilder();
    }

    public DefaultChannelMapping(Map<String, String> mappings) {
        this.mappings = mappings;
    }

    @Override
    public String transform(String channel) {
        return mappings.getOrDefault(channel, channel);
    }
}
