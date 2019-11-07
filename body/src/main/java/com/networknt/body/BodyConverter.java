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

public class BodyConverter {
    static Map<String, Object> convert(FormData data) {
        Map<String, Object> map = new HashMap<>();
        for (String key : data) {
            List<Object> list = new ArrayList<>();
            for (FormData.FormValue value : data.get(key)) {
                list.add(value);
            }
            map.put(key, list);
        }
        return map;
    }
}
