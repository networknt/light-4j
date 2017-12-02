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
