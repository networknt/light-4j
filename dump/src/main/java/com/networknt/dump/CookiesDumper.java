package com.networknt.dump;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;

import java.util.*;

public class CookiesDumper extends AbstractFilterableDumper {
    private Map<String, Object> cookieMap = new LinkedHashMap<>();

    CookiesDumper(Object parentConfig, HttpServerExchange exchange, HttpMessageType type) {
        super(parentConfig, exchange, type);
    }

    @Override
    public Map<String, Object> getResult() {
        return this.cookieMap;
    }

    @Override
    public void putResultTo(Map<String, Object> result) {
        if(this.cookieMap.size() > 0) {
            result.put(DumpConstants.COOKIES, cookieMap);
        }
    }

    @Override
    protected void loadConfig() {
        super.loadConfig();
        if(parentConfig instanceof Map<?, ?>) {
            loadEnableConfig(DumpConstants.COOKIES);
            loadFilterConfig(DumpConstants.FILTERED_COOKIES);
        }
    }

    @Override
    public void dump() {
        if(isApplicable()) {
            Map<String, Cookie> cookiesMap = type.equals(IDumpable.HttpMessageType.RESPONSE) ? exchange.getResponseCookies() : exchange.getRequestCookies();
            cookiesMap.forEach((key, cookie) -> {
                if(!this.filter.contains(cookie.getName())) {
                    Map<String, String> cookieInfo = new HashMap();
                    cookieInfo.put(cookie.getName(), cookie.getValue());
                    this.cookieMap.put(key, cookieInfo);
                }
            });
        }
    }
}
