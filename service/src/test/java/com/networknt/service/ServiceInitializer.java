package com.networknt.service;

public class ServiceInitializer {

    public ChannelMapping channelMapping() {
        IntegrationData data = new IntegrationData();
        return DefaultChannelMapping.builder()
                .with("ReplyTo", data.getAggregateDestination())
                .with("customerService", data.getCommandChannel())
                .build();
    }

}
