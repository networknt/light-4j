package com.networknt.utility;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Map utility class for case-insensitive operations.
 */
public class MapUtil {
    private MapUtil() {
    }

    /**
     * Gets a value from a map by case-insensitive key.
     * @param map map
     * @param key key
     * @param <V> value type
     * @return Optional of value
     */
    public static <V> Optional<V> getValueIgnoreCase(Map<String, V> map, String key) {
        for (Map.Entry<String, V> entry : map.entrySet()) {
            if (Objects.equals(entry.getKey().toLowerCase(), key.toLowerCase())) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    /**
     * Deletes a value from a map by case-insensitive key.
     * @param map map
     * @param key key
     * @param <V> value type
     * @return Optional of deleted value
     */
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
