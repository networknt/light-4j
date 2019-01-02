package com.networknt.dump;

import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractDumper{
    protected final Object parentConfig;
    protected boolean enabled = false;
    protected boolean maskEnabled;
    protected final HttpServerExchange exchange;
    protected Object config;
    private final Logger logger = LoggerFactory.getLogger(AbstractDumper.class);
    public AbstractDumper(Object parentConfig, HttpServerExchange exchange, Boolean maskEnabled) {
        this.parentConfig = parentConfig;
        this.exchange = exchange;
        this.maskEnabled = maskEnabled;
        loadConfig();
    }

    abstract protected void loadConfig();

    abstract protected void putDumpInfoTo(Map<String, Object> result);

    /**
     * @param optionName should be supported dumper type inside com.networknt.DumpConstants
     * set this.config and this.isEnabled based on dumper config
     * the dumper should be enabled when the dumper option is "true", or it has child options
     */
    protected void loadEnableConfig(String optionName) {
        if(this.parentConfig instanceof Map) {
            if(DumpHelper.checkIfOptionTruthy((Map)this.parentConfig, optionName)){
                this.enabled = true;
            }
            Object option = ((Map) this.parentConfig).get(optionName);
            this.config = option == null ? new HashMap<>() : option;
        } else {
            logger.error("parent config of {} should have child options", optionName);
        }
    }

    protected Boolean isEnabled() {
        return this.enabled;
    }

    protected Boolean isMaskEnabled() { return this.maskEnabled; }
}
