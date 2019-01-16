package com.networknt.dump;

import com.networknt.mask.Mask;
import com.networknt.utility.StringUtils;
import io.undertow.server.HttpServerExchange;

import java.util.Map;

/**
 * UrlDumper is to dump url info to result.
 */
public class UrlDumper extends AbstractDumper implements IRequestDumpable{
    private String url = "";

    public UrlDumper(DumpConfig config, HttpServerExchange exchange) {
        super(config, exchange);
    }

    @Override
    public void dumpRequest(Map<String, Object> result) {
        this.url = exchange.getRequestURL();
        if(config.isMaskEnabled()) {
            Mask.maskString(url, "uri");
        }
        this.putDumpInfoTo(result);
    }

    /**
     * put this.url to result
     * @param result a Map you want to put dumping info to.
     */
    @Override
    protected void putDumpInfoTo(Map<String, Object> result) {
        if(StringUtils.isNotBlank(this.url)) {
            result.put(DumpConstants.URL, this.url);
        }
    }

    @Override
    public boolean isApplicableForRequest() {
        return config.isRequestUrlEnabled();
    }
}
