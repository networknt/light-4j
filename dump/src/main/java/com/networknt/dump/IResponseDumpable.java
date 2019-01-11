package com.networknt.dump;

import java.util.Map;

interface IResponseDumpable {
    /**
     * @param result A map you want to put dump information to
     */
    void dumpResponse(Map<String, Object> result);

    /**
     * @return true if dumper is enabled for response
     */
    boolean isApplicableForResponse();
}
