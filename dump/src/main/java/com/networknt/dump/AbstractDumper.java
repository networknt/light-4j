package com.networknt.dump;

import io.undertow.server.HttpServerExchange;

import java.util.Map;

public abstract class AbstractDumper implements IDumpable{
    protected final Object parentConfig;
    protected boolean isEnabled = false;
    protected final HttpMessageType type;
    protected final HttpServerExchange exchange;
    protected Object config;

    public AbstractDumper(Object parentConfig, HttpServerExchange exchange, HttpMessageType type) {
        this.parentConfig = parentConfig;
        this.type = type;
        this.exchange = exchange;
        loadConfig();
    }

    protected void loadConfig() {
        if (parentConfig instanceof Boolean && (Boolean) parentConfig) {
            this.isEnabled = true;
            this.config = true;
        }
    }

    /**
     * @param optionName should be supported dumper type inside com.networknt.dump.DumpConstants
     */
    protected void loadEnableConfig(String optionName) {
        Object config = ((Map) parentConfig).get(optionName);
        if(config instanceof Boolean && (Boolean) config) {
            this.config = true;
            this.isEnabled = true;
        }
    }

    protected Boolean isApplicable() {
        return this.isEnabled;
    }


}
