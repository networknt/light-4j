package org.owasp.encoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A wrapper class that simplify the invocation to encode method. It is located in this package to invoke a non-public
 * method in Encode class.
 *
 * @author Steve Hu
 */
public class EncoderWrapper {

    private final Encoder encoder;
    private final List<String> attributesToIgnore;
    private final List<String> attributesToAppreciate;

    public EncoderWrapper(Encoder encoder, List<String> attributesToIgnore, List<String> attributesToAppreciate) {
        this.encoder = encoder;
        this.attributesToIgnore = attributesToIgnore == null ? new ArrayList<>() : attributesToIgnore;
        this.attributesToAppreciate = attributesToAppreciate == null ? new ArrayList<>() : attributesToAppreciate;
    }

    public void encodeNode(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            if (attributesToIgnore.contains(key)) {
                continue;
            }
            if (!attributesToAppreciate.isEmpty() && !attributesToAppreciate.contains(key)) {
                continue;
            }

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
        return Encode.encode(encoder, value);
    }
}
