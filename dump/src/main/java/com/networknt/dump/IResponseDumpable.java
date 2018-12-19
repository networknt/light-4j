package com.networknt.dump;

import java.util.Map;

public interface IResponseDumpable {
    /**
     * @param result A map you want to put dump information to
     */
    void dumpResponse(Map<String, Object> result);
}
