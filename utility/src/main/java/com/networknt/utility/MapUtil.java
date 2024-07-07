package com.networknt.utility;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class MapUtil {
    // Method to get value from HashMap with case-insensitive key lookup
    public static <V> Optional<V> getValueIgnoreCase(Map<String, V> map, String key) {
        for (Map.Entry<String, V> entry : map.entrySet()) {
            if (Objects.equals(entry.getKey().toLowerCase(), key.toLowerCase())) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    // Method to delete value from HashMap with case-insensitive key lookup
    public static <V> Optional<V> delValueIgnoreCase(Map<String, V> map, String key) {
        for(Iterator<Map.Entry<String, V>> it = map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, V> entry = it.next();
            if (Objects.equals(entry.getKey().toLowerCase(), key.toLowerCase())) {
                it.remove();
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

}
