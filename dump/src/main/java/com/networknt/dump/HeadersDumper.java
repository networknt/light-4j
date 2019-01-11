package com.networknt.dump;

import com.networknt.mask.Mask;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Header Dumper is to dump http request/response header info to result.
 */
public class HeadersDumper extends AbstractDumper implements IRequestDumpable, IResponseDumpable {
    private Map<String, Object> headerMap = new LinkedHashMap<>();
    public HeadersDumper(DumpConfig config, HttpServerExchange exchange) {
        super(config, exchange);
    }

    /**
     * put headerMap to result.
     * @param result a Map<String, Object> you want to put dumping info to.
     */
    @Override
    protected void putDumpInfoTo(Map<String, Object> result) {
        if(this.headerMap.size() > 0) {
            result.put(DumpConstants.HEADERS, this.headerMap);
        }
    }

    /**
     * impl of dumping request headers to result
     * @param result A map you want to put dump information to
     */
    @Override
    public void dumpRequest(Map<String, Object> result) {
        HeaderMap headers = exchange.getRequestHeaders();
        dumpHeaders(headers);
        if(config.isMaskEnabled()) {
            this.headerMap.forEach((s, o) -> headerMap.put(s, Mask.maskRegex((String) o, "requestHeader", s)));
        }
        this.putDumpInfoTo(result);
    }

    /**
     * impl of dumping response headers to result
     * @param result A map you want to put dump information to
     */
    @Override
    public void dumpResponse(Map<String, Object> result) {
        HeaderMap headers = exchange.getResponseHeaders();
        dumpHeaders(headers);
        if(config.isMaskEnabled()) {
            this.headerMap.forEach((s, o) -> headerMap.put(s, Mask.maskRegex((String) o, "responseHeader", s)));
        }
        this.putDumpInfoTo(result);
    }

    /**
     * put headers info to headerMap
     * @param headers types: HeaderMap, get from response or request
     */
    private void dumpHeaders(HeaderMap headers) {
        headers.forEach((headerValues) -> headerValues.forEach((headerValue) -> {
            String headerName = headerValues.getHeaderName().toString();
            if(!config.getRequestFilteredHeaders().contains(headerName)) {
                headerMap.put(headerName, headerValue);
            }
        }));
    }

    @Override
    public boolean isApplicableForRequest() {
        return config.isRequestHeaderEnabled();
    }

    @Override
    public boolean isApplicableForResponse() {
        return config.isResponseHeaderEnabled();
    }
}
