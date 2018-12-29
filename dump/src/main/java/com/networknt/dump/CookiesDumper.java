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
        loadEnableConfig(DumpConstants.COOKIES);
        loadFilterConfig(DumpConstants.FILTERED_COOKIES);
    }

    @Override
    public void dump() {
        if(isApplicable()) {
            Map<String, Cookie> cookiesMap = type.equals(IDumpable.HttpMessageType.RESPONSE) ? exchange.getResponseCookies() : exchange.getRequestCookies();
            cookiesMap.forEach((key, cookie) -> {
                if(!this.filter.contains(cookie.getName())) {
                    List<Map<String, String>> cookieInfoList = new ArrayList<>();
                    cookieInfoList.add(new HashMap<String, String>(){{put(cookie.getName(), cookie.getValue());}});
                    cookieInfoList.add(new HashMap<String, String>(){{put(DumpConstants.COOKIE_DOMAIN, cookie.getDomain());}});
                    cookieInfoList.add(new HashMap<String, String>(){{put(DumpConstants.COOKIE_PATH, cookie.getPath());}});
                    cookieInfoList.add(new HashMap<String, String>(){{put(DumpConstants.COOKIE_EXPIRES, cookie.getExpires() == null ? "" : cookie.getExpires().toString());}});
                    this.cookieMap.put(key, cookieInfoList);
                }
            });
        }
    }
}
