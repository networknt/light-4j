package com.networknt.httpstring;

import java.util.Objects;

public class CacheTask {
    String name;
    String key;

    public CacheTask(String name, String key) {
        this.name = name;
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CacheTask cacheTask = (CacheTask) o;
        return Objects.equals(name, cacheTask.name) && Objects.equals(key, cacheTask.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, key);
    }
}
