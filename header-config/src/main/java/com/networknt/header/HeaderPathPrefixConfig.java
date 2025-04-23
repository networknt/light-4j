package com.networknt.header;

import com.networknt.config.schema.ArrayField;
import com.networknt.config.schema.MapField;

public class HeaderPathPrefixConfig {
    @ArrayField(
            configFieldName = "remove",
            description = "Modify the request for this specific path",
            items = String.class
    )
    private HeaderRequestConfig request;

    @MapField(
            configFieldName = "update",
            description = "Modify the response for this specific path",
            valueType = String.class
    )
    private HeaderResponseConfig response;

    public HeaderRequestConfig getRequest() {
        return request;
    }

    public HeaderResponseConfig getResponse() {
        return response;
    }
}
