package com.networknt.client.circuitbreaker;

enum State {
    CLOSE,
    HALF_OPEN,
    OPEN
}
