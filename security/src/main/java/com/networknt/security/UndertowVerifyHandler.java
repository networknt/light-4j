package com.networknt.security;

import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;

public class UndertowVerifyHandler {
    /**
     * Checks to see if the current exchange type is Upgrade.
     * Two conditions required for a valid upgrade request.
     * - 'Connection' header is set to 'upgrade'.
     * - 'Upgrade' is present.
     *
     * @param headerMap - map containing all exchange headers
     * @return - returns true if the request is an Upgrade request.
     */
    public boolean checkForH2CRequest(HeaderMap headerMap) {
        return  headerMap.getFirst(Headers.UPGRADE) != null
                && headerMap.getFirst(Headers.CONNECTION) != null
                && headerMap.getFirst(Headers.CONNECTION).equalsIgnoreCase("upgrade");
    }

}
