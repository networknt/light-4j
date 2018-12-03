package com.networknt.dump;

import com.networknt.utility.StringUtils;
import io.undertow.server.HttpServerExchange;

import java.util.Map;

public class StatusCodeDumper extends AbstractDumper{
    private String statusCodeResult = "";

    public StatusCodeDumper(Object parentConfig, HttpServerExchange exchange, IDumpable.HttpMessageType type) {
        super(parentConfig, exchange, type);
    }

    @Override
    public String getResult() {
        return this.statusCodeResult;
    }

    @Override
    public void putResultTo(Map<String, Object> result) {
        if(StringUtils.isNotBlank(this.statusCodeResult)) {
            result.put(DumpConstants.STATUS_CODE, this.statusCodeResult);
        }
    }

    @Override
    protected void loadConfig() {
        loadEnableConfig(DumpConstants.STATUS_CODE);
    }

    @Override
    public void dump() {
        if(isApplicable()) {
            this.statusCodeResult = String.valueOf(exchange.getStatusCode());
        }
    }

    @Override
    protected Boolean isApplicable() {
        if(this.type.equals(IDumpable.HttpMessageType.REQUEST)) {
            return false;
        }
        return super.isApplicable();
    }
}
