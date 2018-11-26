package com.networknt.dump;

import java.util.List;
import java.util.Map;

interface IDumpable {
    enum HttpMessageType {
        RESPONSE,
        REQUEST
    }
    default void dumpOption(Boolean configObject){}

    default void dumpOption(Map configObject){}

    default void dumpOption(List<?> configObject){}
}
