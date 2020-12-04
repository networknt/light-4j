package com.networknt.audit;

import static io.undertow.util.PathTemplateMatch.ATTACHMENT_KEY;

import java.util.Map;

import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.utility.ModuleRegistry;

import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.CookieImpl;
import io.undertow.util.Headers;
import io.undertow.util.PathTemplateMatcher;

/**
 * Simulate com.networknt.handler.Handler.start()
 * @author Daniel Zhao
 *
 */
public class ParameterHandler implements MiddlewareHandler {
    private static PathTemplateMatcher<String> pathTemplateMatcher = new PathTemplateMatcher<>();
    private volatile HttpHandler next;

    static {
        pathTemplateMatcher.add("/pet", "0");
        pathTemplateMatcher.add("/pet/{petId}", "1");
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        try {
            String cookieString = exchange.getRequestHeaders().get(Headers.COOKIE).toString();
            cookieString = cookieString.substring(1, cookieString.length() - 1);
            String name = cookieString.split("=")[0];
            String value = cookieString.split("=")[1];
            Cookie cookie = new CookieImpl(name, value);
            exchange.getRequestCookies().put(name, cookie);
        } catch (Exception e) {
        }
        PathTemplateMatcher.PathMatchResult<String> result = pathTemplateMatcher.match(exchange.getRequestPath());

        if (result != null) {
            exchange.putAttachment(ATTACHMENT_KEY,
                    new io.undertow.util.PathTemplateMatch(result.getMatchedTemplate(), result.getParameters()));
            for (Map.Entry<String, String> entry : result.getParameters().entrySet()) {
                exchange.addQueryParam(entry.getKey(), entry.getValue());

                exchange.addPathParam(entry.getKey(), entry.getValue());
            }
        }

        Handler.next(exchange, next);
    }

    @Override
    public HttpHandler getNext() {
        return next;
    }

    @Override
    public MiddlewareHandler setNext(HttpHandler next) {
        Handlers.handlerNotNull(next);
        this.next = next;
        return this;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(ParameterHandler.class.getName(), null, null);
    }

}