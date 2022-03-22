package com.networknt.limit;

public enum LimitKey {

    SERVER("server"), ADDRESS("address"), CLIENT("client");

    private String value;

    LimitKey(String value) {
        this.value = value;
    }

    public static LimitKey fromValue(String key) {
        LimitKey limitKey = null;
        for (LimitKey v: LimitKey.values()) {
            if(v.value.equalsIgnoreCase(key)) {
                limitKey = v;
                break;
            }
        }
        if (limitKey==null) {
            throw new IllegalArgumentException("Invalid config Limit key type :" + key);
        }
        return limitKey;
    }

}
