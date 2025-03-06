package com.networknt.cors;

import com.networknt.config.schema.ArrayField;

import java.util.List;

public class CorsPathPrefix {

    @ArrayField(
            configFieldName = "allowedOrigins",
            description = "List of allowed origins for CORS requests.",
            items = String.class
    )
    List<String> allowedOrigins;

    @ArrayField(
            configFieldName = "allowedMethods",
            description = "List of allowed methods for CORS requests.",
            items = String.class
    )
    List<String> allowedMethods;

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public List<String> getAllowedMethods() {
        return allowedMethods;
    }
}
