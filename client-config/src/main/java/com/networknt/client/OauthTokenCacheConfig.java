package com.networknt.client;
import com.networknt.config.schema.IntegerField;

public class OauthTokenCacheConfig {

    public static final String CAPACITY = "capacity";
    @IntegerField(
            configFieldName = CAPACITY,
            externalizedKeyName = "tokenCacheCapacity",
            defaultValue = 200,
            description = "capacity of caching tokens in the client for downstream API calls."
    )
    private int capacity;

    public int getCapacity() {
        return capacity;
    }
}
