package com.networknt.basicauth;

import com.networknt.config.ConfigInjection;

import java.util.List;

public class UserAuth {
    String username;
    String password;
    List<String> paths;

    public UserAuth(String username, String password, List<String> paths) {
        this.username = username;
        this.password = password;
        this.paths = paths;
    }

    public UserAuth() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = (String) ConfigInjection.decryptEnvValue(ConfigInjection.getDecryptor(), password);
    }

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }
}
