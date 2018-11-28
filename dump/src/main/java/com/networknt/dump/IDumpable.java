package com.networknt.dump;

import java.util.Map;

interface IDumpable {
    enum HttpMessageType {
        RESPONSE,
        REQUEST
    }

    //Dumper dump into their own result
    void dump();

    //Dumper put their own result to passed in result
    void putResultTo(Map<String, Object> result);

    Object getResult();
}
