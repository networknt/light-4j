package com.networknt.service;

public class IntegrationData {
    private long now = System.currentTimeMillis();
    private String commandDispatcherId = "command-dispatcher-" + now;
    private String commandChannel = "command-channel-" + now;
    private String aggregateDestination = "aggregate-destination-" + now;
    private String eventDispatcherId  = "event-dispatcher-" + now;

    public String getAggregateDestination() {
        return aggregateDestination;
    }


    public String getCommandDispatcherId() {
        return commandDispatcherId;
    }

    public String getCommandChannel() {
        return commandChannel;
    }

    public String getEventDispatcherId() {
        return eventDispatcherId;
    }
}
