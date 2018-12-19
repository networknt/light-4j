package com.networknt.dump;

import com.networknt.utility.StringUtils;
import io.undertow.server.HttpServerExchange;

import java.util.Map;

public class StatusCodeDumper extends AbstractDumper implements IResponseDumpable{
    private String statusCodeResult = "";

    public StatusCodeDumper(Object parentConfig, HttpServerExchange exchange) {
        super(parentConfig, exchange);
    }

    @Override
    protected void loadConfig() {
        loadEnableConfig(DumpConstants.STATUS_CODE);
    }


    @Override
    public void dumpResponse(Map<String, Object> result) {
        if(!isEnabled()) {
            return;
        }
        this.statusCodeResult = String.valueOf(exchange.getStatusCode());
        this.putDumpInfoTo(result);
    }

    @Override
    public void putDumpInfoTo(Map<String, Object> result) {
        if(StringUtils.isNotBlank(this.statusCodeResult)) {
            result.put(DumpConstants.STATUS_CODE, this.statusCodeResult);
        }
    }
}
