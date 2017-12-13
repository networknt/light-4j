package com.networknt.service;

public class ServiceInitializer {

    public IntegrationData integrationData() {
        return new IntegrationData();
    }
    public ChannelMapping channelMapping() {
        IntegrationData data = SingletonServiceFactory.getBean(IntegrationData.class);
        return DefaultChannelMapping.builder()
                .with("ReplyTo", data.getAggregateDestination())
                .with("customerService", data.getCommandChannel())
                .build();
    }

}
