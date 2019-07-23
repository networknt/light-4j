/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.body;

import io.undertow.server.handlers.form.FormData;

import java.util.*;

/**
 * To convert the form into a map, we first convert the formValue into a String and then put into the map.
 * For the same form key, there might be multiple form values, we handle it differently based on the size.
 */
public class BodyConverter {
    static Map<String, Object> convert(FormData data) {
        Map<String, Object> map = new HashMap<>();
        for (String key : data) {
            if (data.get(key).size() == 1) {
                String value = data.getFirst(key).getValue();
                map.put(key, value);
            } else if (data.get(key).size() > 1) {
                List<Object> list = new ArrayList<>();
                for (FormData.FormValue value : data.get(key)) {
                    list.add(value.getValue());
                }
                map.put(key, list);
            }
            // ignore size == 0
        }
        return map;
    }
}
