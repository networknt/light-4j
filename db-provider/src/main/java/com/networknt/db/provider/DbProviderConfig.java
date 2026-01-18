package com.networknt.db.provider;

import com.networknt.config.Config;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.IntegerField;
import com.networknt.config.schema.OutputFormat;
import com.networknt.config.schema.StringField;

import java.util.Map;

@ConfigSchema(configKey = "db-provider", configName = "db-provider", outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD})
public class DbProviderConfig {
    public static final String CONFIG_NAME = "db-provider";
    public static final String DRIVER_CLASS_NAME = "driverClassName";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String JDBC_URL = "jdbcUrl";
    public static final String MAXIMUM_POOL_SIZE = "maximumPoolSize";

    @StringField(
            configFieldName = DRIVER_CLASS_NAME,
            externalizedKeyName = DRIVER_CLASS_NAME,
            defaultValue = "org.postgresql.Driver",
            description = "The driver class name for the database connection."
    )
    String driverClassName;

    @StringField(
            configFieldName = JDBC_URL,
            externalizedKeyName = JDBC_URL,
            defaultValue = "jdbc:postgresql://timescale:5432/configserver",
            description = "JDBC connection URL"
    )
    String jdbcUrl;

    @StringField(
            configFieldName = USERNAME,
            externalizedKeyName = USERNAME,
            defaultValue = "postgres",
            description = "JDBC connection username"
    )
    String username;

    @StringField(
            configFieldName = PASSWORD,
            externalizedKeyName = PASSWORD,
            defaultValue = "secret",
            description = "JDBC connection password"
    )
    char[] password;

    @IntegerField(
            configFieldName = MAXIMUM_POOL_SIZE,
            externalizedKeyName = MAXIMUM_POOL_SIZE,
            defaultValue = "3",
            description = "Maximum number of connections in the pool"
    )
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
        return new String(password);
    }

    public void setPassword(String password) {
        this.password = password.toCharArray();
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
        if(object != null) password = ((String)object).toCharArray();
        object = mappedConfig.get(JDBC_URL);
        if(object != null) jdbcUrl = (String)object;
        object = mappedConfig.get(MAXIMUM_POOL_SIZE);
        if(object != null) Config.loadIntegerValue(MAXIMUM_POOL_SIZE, object);
    }
}
