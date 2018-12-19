package com.networknt.dump;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;

import java.util.*;

public class CookiesDumper extends AbstractFilterableDumper implements IRequestDumpable, IResponseDumpable{
    private Map<String, Object> cookieMap = new LinkedHashMap<>();

    CookiesDumper(Object parentConfig, HttpServerExchange exchange) {
        super(parentConfig, exchange);
    }

    @Override
    protected void loadConfig() {
        loadEnableConfig(DumpConstants.COOKIES);
        loadFilterConfig(DumpConstants.FILTERED_COOKIES);
    }

    @Override
    public void dumpRequest(Map<String, Object> result) {
        if(!isEnabled()) {
            return;
        }
        Map<String, Cookie> cookiesMap = exchange.getRequestCookies();
        dumpCookies(cookiesMap);
        this.putDumpInfoTo(result);

    }

    @Override
    public void dumpResponse(Map<String, Object> result) {
        if(!isEnabled()) {
            return;
        }
        Map<String, Cookie> cookiesMap = exchange.getResponseCookies();
        dumpCookies(cookiesMap);
        this.putDumpInfoTo(result);
    }

    /**
     * put cookies info to cookieMap
     * @param cookiesMap Map of cookies
     */
    private void dumpCookies(Map<String, Cookie> cookiesMap) {
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

    @Override
    public void putDumpInfoTo(Map<String, Object> result) {
        if(this.cookieMap.size() > 0) {
            result.put(DumpConstants.COOKIES, cookieMap);
        }
    }
}
