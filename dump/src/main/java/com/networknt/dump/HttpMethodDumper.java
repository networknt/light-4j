package com.networknt.dump;

import io.undertow.server.HttpServerExchange;

import java.util.*;

import static com.networknt.dump.DumpConstants.REQUEST;
import static com.networknt.dump.DumpConstants.RESPONSE;

public class HttpMethodDumper extends AbstractDumper {
    private Map<String, Object> httpMethodMap = new LinkedHashMap<>();
    private List<IDumpable> childDumpers;

    HttpMethodDumper(Object config, HttpServerExchange exchange, HttpMessageType type) {
        super(config, exchange, type);
    }

    @Override
    protected void loadConfig() {
        super.loadConfig();
        if(parentConfig instanceof Map<?, ?>) {
            if(this.type == HttpMessageType.RESPONSE) {
                //when response: true
                loadEnableConfig(DumpConstants.RESPONSE);
                this.config = ((Map) parentConfig).get(DumpConstants.RESPONSE);
            } else {
                loadEnableConfig(DumpConstants.REQUEST);
                this.config = ((Map) parentConfig).get(DumpConstants.REQUEST);
            }
            if(this.config instanceof Map<?, ?>) {
                this.isEnabled = true;
            }
        }
    }

    @Override
    public void dump() {
        if(isApplicable()) {
            if(this.childDumpers == null || this.childDumpers.size() == 0 ) {
                initializeChildDumpers();
            }
            childDumpers.forEach(dumper -> {
                dumper.dump();
                dumper.putResultTo(httpMethodMap);
            });
        }
    }

    @Override
    public Map<String, Object> getResult() {
        return this.httpMethodMap;
    }

    @Override
    public void putResultTo(Map<String, Object> result) {
        if(this.httpMethodMap.size() > 0) {
            if(this.type == HttpMessageType.RESPONSE) {
                result.put(RESPONSE, this.httpMethodMap);
            } else {
                result.put(REQUEST, this.httpMethodMap);
            }
        }
    }

    private void initializeChildDumpers() {
        IDumpable bodyDumper = new BodyDumper(config, exchange, type);
        IDumpable cookiesDumper = new CookiesDumper(config, exchange, type);
        IDumpable headersDumper = new HeadersDumper(config, exchange, type);
        IDumpable queryParametersDumper = new QueryParametersDumper(config, exchange, type);
        IDumpable statusCodeDumper = new StatusCodeDumper(config, exchange, type);
        this.childDumpers = new ArrayList<>(Arrays.asList(bodyDumper, cookiesDumper, headersDumper, queryParametersDumper, statusCodeDumper));
    }
}
