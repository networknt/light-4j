package com.networknt.db.provider;

import com.networknt.config.Config;

import java.util.Map;

public class DbProviderConfig {
    public static final String CONFIG_NAME = "db-provider";
    public static final String DRIVER_CLASS_NAME = "driverClassName";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String JDBC_URL = "jdbcUrl";
    public static final String MAXIMUM_POOL_SIZE = "maximumPoolSize";
    String driverClassName;
    String username;
    String password;
    String jdbcUrl;
    int maximumPoolSize;

    private final Config config;
    private Map<String, Object> mappedConfig;

    private DbProviderConfig() {
        this(CONFIG_NAME);
    }

    /**
     * Please note that this constructor is only for testing to load different config files
     * to test different configurations.
     * @param configName String
     */
    private DbProviderConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
    }
    public static DbProviderConfig load() {
        return new DbProviderConfig();
    }

    public static DbProviderConfig load(String configName) {
        return new DbProviderConfig(configName);
    }

    void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
    }
    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
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
        this.password = password;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    private void setConfigData() {
        Object object = mappedConfig.get(DRIVER_CLASS_NAME);
        if(object != null) driverClassName = (String)object;
        object = mappedConfig.get(USERNAME);
        if(object != null) username = (String)object;
        object = mappedConfig.get(PASSWORD);
        if(object != null) password = (String)object;
        object = mappedConfig.get(JDBC_URL);
        if(object != null) jdbcUrl = (String)object;
        object = mappedConfig.get(MAXIMUM_POOL_SIZE);
        if(object != null) Config.loadIntegerValue(MAXIMUM_POOL_SIZE, object);
    }
}
