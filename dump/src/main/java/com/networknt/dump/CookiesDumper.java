/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.dump;

import com.networknt.mask.Mask;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;

import java.util.*;
/**
 * CookiesDumper is to dump http request/response cookie info to result.
 */
public class CookiesDumper extends AbstractDumper implements IRequestDumpable, IResponseDumpable{
    private Map<String, Object> cookieMap = new LinkedHashMap<>();

    CookiesDumper(DumpConfig config, HttpServerExchange exchange) {
        super(config, exchange);
    }

    /**
     * impl of dumping request cookies to result
     * @param result A map you want to put dump information to
     */
    @Override
    public void dumpRequest(Map<String, Object> result) {
        Iterable<Cookie> iterable = exchange.requestCookies();
        dumpCookies(iterable, "requestCookies");
        this.putDumpInfoTo(result);

    }

    /**
     * impl of dumping response cookies to result
     * @param result A map you want to put dump information to
     */
    @Override
    public void dumpResponse(Map<String, Object> result) {
        Iterable<Cookie> iterable = exchange.responseCookies();
        dumpCookies(iterable, "responseCookies");
        this.putDumpInfoTo(result);
    }

    /**
     * put cookies info to cookieMap
     * @param iterable Iterable of cookies
     */
    private void dumpCookies(Iterable<Cookie> iterable, String maskKey) {
        Iterator<Cookie> iterator = iterable.iterator();
        while(iterator.hasNext()) {
            Cookie cookie = iterator.next();
            if(!config.getRequestFilteredCookies().contains(cookie.getName())) {
                List<Map<String, String>> cookieInfoList = new ArrayList<>();
                //mask cookieValue
                String cookieValue = config.isMaskEnabled() ? Mask.maskRegex(cookie.getValue(), maskKey, cookie.getName()) : cookie.getValue();
                cookieInfoList.add(new HashMap<String, String>(){{put(DumpConstants.COOKIE_VALUE, cookieValue);}});
                cookieInfoList.add(new HashMap<String, String>(){{put(DumpConstants.COOKIE_DOMAIN, cookie.getDomain());}});
                cookieInfoList.add(new HashMap<String, String>(){{put(DumpConstants.COOKIE_PATH, cookie.getPath());}});
                cookieInfoList.add(new HashMap<String, String>(){{put(DumpConstants.COOKIE_EXPIRES, cookie.getExpires() == null ? "" : cookie.getExpires().toString());}});
                this.cookieMap.put(cookie.getName(), cookieInfoList);
            }

        }
    }

    /**
     * put cookieMap to result
     * @param result a Map you want to put dumping info to.
     */
    @Override
    protected void putDumpInfoTo(Map<String, Object> result) {
        if(this.cookieMap.size() > 0) {
            result.put(DumpConstants.COOKIES, cookieMap);
        }
    }

    @Override
    public boolean isApplicableForRequest() {
        return config.isRequestCookieEnabled();
    }

    @Override
    public boolean isApplicableForResponse() {
        return config.isResponseCookieEnabled();
    }
}
