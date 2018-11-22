package com.networknt.decode;

import java.util.List;

public class RequestDecodeConfig {
    public static final String CONFIG_NAME = "request-decode";

    boolean enabled;
    List<String> decoders;

    public RequestDecodeConfig() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getDecoders() {
        return decoders;
    }

    public void setDecoders(List<String> decoders) {
        this.decoders = decoders;
    }
}
