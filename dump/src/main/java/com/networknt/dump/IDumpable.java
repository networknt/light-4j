package com.networknt.dump;

import java.util.List;
import java.util.Map;

interface IDumpable {
    enum HttpMessageType {
        RESPONSE("response"),
        REQUEST("request");

        private String type;

        HttpMessageType(String type) {
            this.type = type;
        }

        public String value() {
            return this.type;
        }
    }

    default void dumpOption(Boolean configObject){}

    default void dumpOption(Map configObject){}

    default void dumpOption(List<?> configObject){}

    Map<String, Object> getResult();
}
