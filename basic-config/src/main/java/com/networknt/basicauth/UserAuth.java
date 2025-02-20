package com.networknt.basicauth;

import com.networknt.config.ConfigInjection;
import com.networknt.config.schema.ArrayField;
import com.networknt.config.schema.StringField;

import java.util.List;

public class UserAuth {

    @StringField(
            configFieldName = "username",
            description = "UserAuth username"
    )
    String username;

    @StringField(
            configFieldName = "password",
            description = "UserAuth password"
    )
    String password;

    @ArrayField(
            configFieldName = "paths",
            description = "The different paths that will be valid for this UserAuth",
            items = String.class
    )
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
