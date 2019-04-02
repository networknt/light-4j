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

package com.networknt.common;

import com.networknt.service.SingletonServiceFactory;
import com.networknt.decrypt.Decryptor;

import java.util.List;
import java.util.Map;

public class DecryptUtil {
    public static Map<String, Object> decryptMap(Map<String, Object> map) {
        decryptNode(map);
        return map;
    }

    private static void decryptNode(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String)
                map.put(key, decryptObject(value));
            else if (value instanceof Map)
                decryptNode((Map) value);
            else if (value instanceof List) {
                decryptList((List)value);
            }
        }
    }

    private static void decryptList(List list) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) instanceof String) {
                list.set(i, decryptObject((list.get(i))));
            } else if(list.get(i) instanceof Map) {
                decryptNode((Map<String, Object>)list.get(i));
            } else if(list.get(i) instanceof List) {
                decryptList((List)list.get(i));
            }
        }
    }

    private static Object decryptObject(Object object) {
        if(object instanceof String) {
            if(((String)object).startsWith(Decryptor.CRYPT_PREFIX)) {
                Decryptor decryptor = SingletonServiceFactory.getBean(Decryptor.class);
                if(decryptor == null) throw new RuntimeException("No implementation of Decryptor is defined in service.yml");
                object = decryptor.decrypt((String)object);
            }

        }
        return object;
    }
}
