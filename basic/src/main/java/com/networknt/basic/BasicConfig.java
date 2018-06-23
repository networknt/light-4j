package com.networknt.basic;

import com.networknt.common.DecryptUtil;

import java.util.List;
import java.util.Map;

public class BasicConfig {
    boolean enabled;
    List<Map<String, Object>> users;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<Map<String, Object>> getUsers() { return users; }
    public void setUsers(List<Map<String, Object>> users) {
        users.forEach(user -> DecryptUtil.decryptMap(user));
        this.users = users;
    }
}
