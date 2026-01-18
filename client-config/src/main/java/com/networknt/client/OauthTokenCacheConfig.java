package com.networknt.client;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.networknt.config.schema.IntegerField;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OauthTokenCacheConfig {

    @IntegerField(
            configFieldName = ClientConfig.CAPACITY,
            externalizedKeyName = "tokenCacheCapacity",
            defaultValue = "200",
            description = "Capacity of caching tokens in the client for downstream API calls. The default value is 200."
    )
    @JsonProperty(ClientConfig.CAPACITY)
    private Integer capacity = 200;

    public Integer getCapacity() {
        return capacity;
    }
}
