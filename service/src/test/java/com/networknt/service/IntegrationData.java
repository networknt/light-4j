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
