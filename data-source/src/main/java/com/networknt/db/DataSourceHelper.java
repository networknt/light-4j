package com.networknt.db;

import com.networknt.common.DecryptUtil;
import com.networknt.config.Config;
import com.zaxxer.hikari.HikariDataSource;

import java.util.Map;

/**
 * This is the generic Hikari datasource and it is suitable if you only have one database
 * in your application. You can still use it when you have one datasource for production
 * and another one for testing by putting two different version of service.yml and datasource.yml
 * in both main/resources/config and test/resources/config.
 *
 * @author Steve Hu
 */
public class DataSourceHelper
{
    public static final String SECRET = "secret";
    public static final String DATASOURCE = "datasource";

    private HikariDataSource _ds;
    private String _dsName;
    private String _dbPassKey;

    public DataSourceHelper(String dsName, String dbPassKey) {
        _dsName = dsName;
        _dbPassKey = dbPassKey;
        _ds = createDataSource(dsName, dbPassKey);
    }

    public String getDsName() { return _dsName; }
    public String getDbPassKey() { return _dbPassKey; }
    public HikariDataSource getDataSource() { return _ds; }

    public static HikariDataSource createDataSource(String dsName, String dbPassKey) {
        // get the configured datasources
        Map<String, Object> dataSourceMap = Config.getInstance().getJsonMapConfig(DATASOURCE);

        // get the decrypted secret file
        Map<String, Object> secret = DecryptUtil.decryptMap(Config.getInstance().getJsonMapConfig(SECRET));

        // get the requested datasource
        Map<String, Object> mainParams = (Map<String, Object>) dataSourceMap.get(dsName);
        Map<String, String> configParams = (Map<String, String>)mainParams.get("parameters");

        // create the DataSource
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl((String)mainParams.get("jdbcUrl"));
        ds.setUsername((String)mainParams.get("username"));

        // use encrypted password
        ds.setPassword((String)secret.get(dbPassKey));

        // set datasource paramters
        ds.setMaximumPoolSize((Integer)mainParams.get("maximumPoolSize"));
        ds.setConnectionTimeout((Integer)mainParams.get("connectionTimeout"));

        // add datasource specific connection parameters
        if(configParams != null) configParams.forEach((k, v) -> ds.addDataSourceProperty(k, v));

        return ds;
    }
}
