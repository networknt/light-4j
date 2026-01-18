package com.networknt.limit;

import com.networknt.config.schema.MapField;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RateLimitSet {

    @MapField(
            configFieldName = "directMaps",
            externalizedKeyName = "directMaps",
            valueTypeOneOf = {
                    Map.class,
                    String.class
            }
    )
    public Map<String, List<LimitQuota>> directMaps;

    public Map<String, List<LimitQuota>> getDirectMaps() {
        return directMaps;
    }

    public void setDirectMaps(Map<String, List<LimitQuota>> directMaps) {
        this.directMaps = directMaps;
    }

    public void addDirectMap(String key, List<LimitQuota> limitQuotas) {
        if (this.directMaps == null) {
            this.directMaps = new HashMap<>();
        }
        this.directMaps.put(key, limitQuotas);
    }
}
