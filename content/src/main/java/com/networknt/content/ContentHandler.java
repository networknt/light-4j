package com.networknt.content;

import com.networknt.config.Config;
import com.networknt.handler.Handler;
import com.networknt.handler.MiddlewareHandler;
import com.networknt.utility.ModuleRegistry;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

/**
 * This is a middleware handler that is responsible for setting the default content-type header
 * if it is empty. This can be enabled with content.yml config file and the default type in the
 * config file is application/json.
 *
 * The other way to do that is to extend your handler from LightHttpHandler use the setExchangeStatus
 * default method for all error status. 
 *
 * Created by Ricardo Pina Arellano on 13/06/18.
 */
public class ContentHandler implements MiddlewareHandler {

  public static final String CONFIG_NAME = "content";

  public static final ContentConfig config = (ContentConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ContentConfig.class);

  private static final String contentType = config.getContentType();

  private volatile HttpHandler next;

  public ContentHandler() { }

  @Override
  public HttpHandler getNext() {
    return next;
  }

  @Override
  public MiddlewareHandler setNext(final HttpHandler next) {
    Handlers.handlerNotNull(next);

    this.next = next;

    return this;
  }

  @Override
  public boolean isEnabled() {
    return config.isEnabled();
  }

  @Override
  public void register() {
    ModuleRegistry.registerModule(ContentConfig.class.getName(), Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME), null);
  }

  @Override
  public void handleRequest(final HttpServerExchange exchange) throws Exception {
    if (exchange.getRequestHeaders().contains(Headers.CONTENT_TYPE)) {
      exchange
        .getResponseHeaders()
        .put(Headers.CONTENT_TYPE, exchange.getRequestHeaders().get(Headers.CONTENT_TYPE).element());
    } else {
      exchange
        .getResponseHeaders()
        .put(Headers.CONTENT_TYPE, contentType);
    }
    Handler.next(exchange, next);
  }
}
