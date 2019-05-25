package com.networknt.sanitizer.enconding;

import com.networknt.sanitizer.EncodingStrategy;
import org.owasp.encoder.Encode;

import java.util.List;
import java.util.Map;

public class Encoding {

    private final EncodingStrategy encodingStrategy;

    public Encoding(EncodingStrategy encodingStrategy) {
        this.encodingStrategy = encodingStrategy;
    }

    public void encodeNode(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String) {
                map.put(key, applyEncoding((String) value));
            } else if (value instanceof Map) {
                encodeNode((Map) value);
        } else if (value instanceof List) {
                encodeList((List)value);
            }
        }
    }

    public void encodeList(List list) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) instanceof String) {
                list.set(i, applyEncoding((String) list.get(i)));
            } else if (list.get(i) instanceof Map) {
                encodeNode((Map<String, Object>)list.get(i));
            } else if (list.get(i) instanceof List) {
                encodeList((List)list.get(i));
            }
        }
    }

    public String applyEncoding(String value) {
        switch (encodingStrategy) {
            case DEFAULT:
                return Encode.forJavaScriptSource(value);
        }
        throw new IllegalStateException("illegal kind of encoding");
    }
}
