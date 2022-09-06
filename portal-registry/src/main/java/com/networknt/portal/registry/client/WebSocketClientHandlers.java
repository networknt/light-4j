package com.networknt.portal.registry.client;

/**
 * Created by leiko on 27/02/15.
 *
 */
public interface WebSocketClientHandlers {

    void onOpen();

    void onMessage(String msg);

    void onClose(int code, String reason);

    void onError(Exception e);
}
