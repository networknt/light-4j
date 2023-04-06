package com.networknt.db.provider;

import com.networknt.cache.CacheManager;
import com.networknt.config.Config;
import com.networknt.server.StartupHookProvider;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Start up hook for the SQL provider to create the datasource and initial the cache.
 * All application/Api can use this in the service.yml to load data source in startup.
 *
 * @author Steve Hu
 */
public class SqlDbStartupHook implements StartupHookProvider {
    private static final Logger logger = LoggerFactory.getLogger(SqlDbStartupHook.class);

    static DbProviderConfig config = (DbProviderConfig) Config.getInstance().getJsonObjectConfig(DbProviderConfig.CONFIG_NAME, DbProviderConfig.class);
    public static HikariDataSource ds;
    // key and json cache for the dropdowns.
    public static CacheManager cacheManager;

    @Override
    public void onStartup() {
        logger.info("SqlDbStartupHook begins");
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName(config.getDriverClassName());
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setJdbcUrl(config.getJdbcUrl());
        if(logger.isTraceEnabled()) logger.trace("jdbcUrl = " + config.getJdbcUrl());
        hikariConfig.setMaximumPoolSize(config.getMaximumPoolSize());
        ds = new HikariDataSource(hikariConfig);
        cacheManager = CacheManager.getInstance();
        logger.info("SqlDbStartupHook ends");

    }
}
