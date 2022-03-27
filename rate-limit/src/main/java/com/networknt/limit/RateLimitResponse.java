package com.networknt.limit;

import java.util.Map;

public class RateLimitResponse {
    boolean allow;
    Map<String, String> headers;

    public RateLimitResponse(boolean isAllow, Map<String, String> headers) {
        this.allow = isAllow;
        this.headers = headers;
    }

    public boolean isAllow() {
        return allow;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
