package com.networknt.dump;

import com.networknt.utility.StringUtils;
import io.undertow.server.HttpServerExchange;

import java.util.Map;

public class StatusCodeDumper extends AbstractDumper implements IResponseDumpable{
    private String statusCodeResult = "";

    public StatusCodeDumper(DumpConfig config, HttpServerExchange exchange) {
        super(config, exchange);
    }

    @Override
    public void dumpResponse(Map<String, Object> result) {
        if(!config.isResponseStatusCodeEnabled()) { return; }

        this.statusCodeResult = String.valueOf(exchange.getStatusCode());
        this.putDumpInfoTo(result);
    }

    @Override
    protected void putDumpInfoTo(Map<String, Object> result) {
        if(StringUtils.isNotBlank(this.statusCodeResult)) {
            result.put(DumpConstants.STATUS_CODE, this.statusCodeResult);
        }
    }
}
