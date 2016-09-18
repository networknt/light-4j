package com.networknt.info;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by steve on 18/09/16.
 */
public class ServerInfoConfig {
    boolean enableServerInfo;

    @JsonIgnore
    String description;

    public ServerInfoConfig() {
    }

    public boolean isEnableServerInfo() {
        return enableServerInfo;
    }

    public void setEnableServerInfo(boolean enableServerInfo) {
        this.enableServerInfo = enableServerInfo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
