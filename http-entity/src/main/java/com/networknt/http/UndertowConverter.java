package com.networknt.http;

import io.undertow.util.HeaderMap;

import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class UndertowConverter {
    public static Map<String, String> convertHeadersToMap(HeaderMap headerMap) {
        Map<String, String> headers = new HashMap<>();
        headerMap.forEach(header -> {
            headers.put(header.getHeaderName().toString(), header.getFirst());
        });
        return headers;
    }

    public static Map<String, String> convertParametersToMap(Map<String, Deque<String>> parameters) {
        Map<String, String> paramMap = new HashMap<>();
        // Iterate over the parameter names and values
        for (Map.Entry<String, Deque<String>> entry : parameters.entrySet()) {
            String paramName = entry.getKey();
            Deque<String> paramValues = entry.getValue();
            if (!paramValues.isEmpty()) {
                // For simplicity, only consider the first value if there are multiple values
                paramMap.put(paramName, paramValues.getFirst());
            }
        }
        return paramMap;
    }

}
