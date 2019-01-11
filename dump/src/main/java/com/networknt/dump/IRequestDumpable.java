package com.networknt.dump;

import java.util.Map;

interface IRequestDumpable {
    /**
     * dump http request info to result
     * @param result A map you want to put dump information to
     */
    void dumpRequest(Map<String, Object> result);

    /**
     * @return true if dumper is enabled for request
     */
    boolean isApplicableForRequest();
}
