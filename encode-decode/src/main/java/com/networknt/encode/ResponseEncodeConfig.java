package com.networknt.encode;

import java.util.List;

public class ResponseEncodeConfig {
    public static final String CONFIG_NAME = "response-encode";

    boolean enabled;
    List<String> encoders;

    public ResponseEncodeConfig() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getEncoders() {
        return encoders;
    }

    public void setEncoders(List<String> encoders) {
        this.encoders = encoders;
    }
}
