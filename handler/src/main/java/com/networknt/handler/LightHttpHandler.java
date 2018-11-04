package com.networknt.handler;

import com.networknt.status.Status;
import com.sun.net.httpserver.HttpServer;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an extension of HttpHandler that provides a default method to handle
 * error status. All API handler should extend from this interface.
 *
 * @author Steve Hu
 */
public interface LightHttpHandler extends HttpHandler {
    Logger logger = LoggerFactory.getLogger(LightHttpHandler.class);
    String ERROR_NOT_DEFINED = "ERR10042";

    /**
     * This method is used to construct a standard error status in JSON format from an error code.
     *
     * @param exchange HttpServerExchange
     * @param code error code
     * @param args arguments for error description
     */
    default void setExchangeStatus(HttpServerExchange exchange, String code, final Object... args) {
        Status status = new Status(code, args);
        if(status.getStatusCode() == 0) {
            // There is no entry in status.yml for this particular error code.
            status = new Status(ERROR_NOT_DEFINED, code);
        }
        exchange.setStatusCode(status.getStatusCode());
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        status.setDescription(status.getDescription().replaceAll("\\\\", "\\\\\\\\"));
        exchange.getResponseSender().send(status.toString());
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        logger.error(status.toString() + " at " + elements[2].getClassName() + "." + elements[2].getMethodName() + "(" + elements[2].getFileName() + ":" + elements[2].getLineNumber() + ")");
    }

    /**
     * There are situations that the downstream service returns an error status response and we just
     * want to bubble up to the caller and eventually to the original caller.
     *
     * @param exchange HttpServerExchange
     * @param status error status
     */
    default void setExchangeStatus(HttpServerExchange exchange, Status status) {
        exchange.setStatusCode(status.getStatusCode());
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        status.setDescription(status.getDescription().replaceAll("\\\\", "\\\\\\\\"));
        exchange.getResponseSender().send(status.toString());
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        logger.error(status.toString() + " at " + elements[2].getClassName() + "." + elements[2].getMethodName() + "(" + elements[2].getFileName() + ":" + elements[2].getLineNumber() + ")");
    }
}
