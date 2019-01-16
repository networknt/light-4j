package com.networknt.dump;

import com.networknt.utility.StringUtils;
import io.undertow.server.HttpServerExchange;

import java.util.Map;

/**
 * StatusCodeDumper is to dump http response status code info to result.
 */
public class StatusCodeDumper extends AbstractDumper implements IResponseDumpable{
    private String statusCodeResult = "";

    public StatusCodeDumper(DumpConfig config, HttpServerExchange exchange) {
        super(config, exchange);
    }

    /**
     * impl of dumping response status code to result
     * @param result A map you want to put dump information to
     */
    @Override
    public void dumpResponse(Map<String, Object> result) {
        this.statusCodeResult = String.valueOf(exchange.getStatusCode());
        this.putDumpInfoTo(result);
    }

    /**
     * put this.statusCodeResult to result
     * @param result a Map you want to put dumping info to.
     */
    @Override
    protected void putDumpInfoTo(Map<String, Object> result) {
        if(StringUtils.isNotBlank(this.statusCodeResult)) {
            result.put(DumpConstants.STATUS_CODE, this.statusCodeResult);
        }
    }

    @Override
    public boolean isApplicableForResponse() {
        return config.isResponseStatusCodeEnabled();
    }

}
