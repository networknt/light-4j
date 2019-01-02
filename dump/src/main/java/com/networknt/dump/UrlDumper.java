package com.networknt.dump;

import com.networknt.mask.Mask;
import com.networknt.utility.StringUtils;
import io.undertow.server.HttpServerExchange;

import java.util.Map;

public class UrlDumper extends AbstractDumper implements IRequestDumpable{
    private String url = "";

    public UrlDumper(Object parentConfig, HttpServerExchange exchange, Boolean maskEnabled) {
        super(parentConfig, exchange, maskEnabled);
    }

    @Override
    protected void loadConfig() {
        loadEnableConfig(DumpConstants.URL);
    }


    @Override
    public void dumpRequest(Map<String, Object> result) {
        if(!isEnabled()) {
            return;
        }
        this.url = exchange.getRequestURL();
        if(isMaskEnabled()) {
            Mask.maskString(url, "uri");
        }
        this.putDumpInfoTo(result);
    }

    @Override
    public void putDumpInfoTo(Map<String, Object> result) {
        if(StringUtils.isNotBlank(this.url)) {
            result.put(DumpConstants.URL, this.url);
        }
    }
}
