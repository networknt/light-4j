package com.networknt.cors;

import com.networknt.config.schema.ArrayField;

import java.util.List;

public class CorsPathPrefix {

    @ArrayField(
            configFieldName = "allowedOrigins",
            description = "List of allowed exact origins (including custom schemes) or HTTP/HTTPS scoped patterns such as https://*.example.com. Trusts all matching subdomains at every depth, excludes the apex, and rejects public/private PSL suffixes. Replaces global origins; the legacy server-origin fallback remains.",
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
