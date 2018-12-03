package com.networknt.dump;

import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public abstract class AbstractDumper implements IDumpable{
    protected final Object parentConfig;
    protected boolean isEnabled = false;
    protected final HttpMessageType type;
    protected final HttpServerExchange exchange;
    protected Object config;
    private Logger logger = LoggerFactory.getLogger(AbstractDumper.class);

    public AbstractDumper(Object parentConfig, HttpServerExchange exchange, HttpMessageType type) {
        this.parentConfig = parentConfig;
        this.type = type;
        this.exchange = exchange;
        loadConfig();
    }

    abstract protected void loadConfig();

    /**
     * @param optionName should be supported dumper type inside com.networknt.dump.DumpConstants
     * set this.config && this.isEnabled based on dumper config
     * the dumper should be enabled when the dumper option is "true", or it has child options
     */
    protected void loadEnableConfig(String optionName) {
        if(parentConfig instanceof Map) {
            Object config = ((Map) parentConfig).get(optionName);
            if((config instanceof Boolean && (Boolean) config)
                    || config instanceof Map) {
                this.isEnabled = true;
            }
            this.config = config;
        } else if(parentConfig instanceof Boolean){
            //e.g. if parentConfig is "false" then child config should extend this option
            this.config = parentConfig;
            this.isEnabled = (Boolean)parentConfig;
        } else {
            logger.error("doesn't support config type for option: {}", optionName);
        }
    }

    protected Boolean isApplicable() {
        return this.isEnabled;
    }
}
