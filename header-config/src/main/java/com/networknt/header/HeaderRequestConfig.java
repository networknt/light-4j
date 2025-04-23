package com.networknt.header;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.networknt.config.schema.ArrayField;
import com.networknt.config.schema.MapField;

import java.util.List;
import java.util.Map;

public class HeaderRequestConfig {
    @ArrayField(
            configFieldName = "remove",
            externalizedKeyName = "request.remove",
            externalized = true,
            description = "Remove all the request headers listed here. The value is a list of keys",
            items = String.class
    )
    @JsonDeserialize(using = HeaderConfig.HeaderRemoveDeserializer.class)
    private List<String> remove;

    @MapField(
            configFieldName = "update",
            externalizedKeyName = "request.update",
            externalized = true,
            description = "List of key value pairs to update headers.",
            valueType = String.class
    )
    @JsonDeserialize(using = HeaderConfig.HeaderUpdateDeserializer.class)
    private Map<String, String> update;

    public List<String> getRemove() {
        return remove;
    }

    public Map<String, String> getUpdate() {
        return update;
    }


}
