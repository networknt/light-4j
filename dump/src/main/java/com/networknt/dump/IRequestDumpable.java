package com.networknt.dump;

import java.util.Map;

public interface IRequestDumpable {
    /**
     * @param result A map you want to put dump information to
     */
    void dumpRequest(Map<String, Object> result);
}
