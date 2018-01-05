package com.networknt.common;

import com.networknt.service.SingletonServiceFactory;
import com.networknt.utility.Decryptor;
import org.owasp.encoder.Encode;

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
