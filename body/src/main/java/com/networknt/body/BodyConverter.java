package com.networknt.body;

import io.undertow.server.handlers.form.FormData;

import java.util.*;

public class BodyConverter {
    static Map<String, Object> convert(FormData data) {
        Map<String, Object> map = new HashMap<>();
        Iterator<String> iterator = data.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            List<Object> list = new ArrayList<>();
            for (FormData.FormValue value : data.get(key)) {
                list.add(value);
            }
            map.put(key, list);
        }
        return map;
    }
}
