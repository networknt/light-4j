package com.networknt.db;

import com.networknt.common.DecryptUtil;
import com.networknt.config.Config;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.Map;

/**
 * This is the generic Hikari datasource and it is suitable if you only have one database
 * in your application. You can still use it when you have one datasource for production
 * and another one for testing by putting two different version of service.yml and datasource.yml
 * in both main/resources/config and test/resources/config.
 *
 * @author Steve Hu
 */
public class GenericDataSource {
    protected static final String DATASOURCE = "datasource";
    protected static final String SECRET = "secret";
    private static final String DB_PASSWORD = "dbPassword";
    private static final String DS_NAME = "H2DataSource";

    // the HikariDataSource
    private HikariDataSource ds;
    // the data source name
    protected String dsName;

    public String getDsName() {
        return dsName;
    }

    public String getDbPassKey() {
        return DB_PASSWORD;
    }

    public GenericDataSource() {
        this.dsName = DS_NAME;
        this.ds = createDataSource();
    }

    public GenericDataSource(String dsName) {
        this.dsName = dsName;
        this.ds = createDataSource();
    }

    protected HikariDataSource createDataSource() {
        // get the configured datasources
        Map<String, Object> dataSourceMap = Config.getInstance().getJsonMapConfig(DATASOURCE);

        // get the decrypted secret file
        Map<String, Object> secret = DecryptUtil.decryptMap(Config.getInstance().getJsonMapConfig(SECRET));

        // get the requested datasource
        Map<String, Object> mainParams = (Map<String, Object>) dataSourceMap.get(getDsName());
        Map<String, String> configParams = (Map<String, String>)mainParams.get("parameters");

        // create the DataSource
        ds = new HikariDataSource();
        ds.setJdbcUrl((String)mainParams.get("jdbcUrl"));
        ds.setUsername((String)mainParams.get("username"));

        // use encrypted password
        ds.setPassword((String)secret.get(getDbPassKey()));

        // set datasource paramters
        ds.setMaximumPoolSize((Integer)mainParams.get("maximumPoolSize"));
        ds.setConnectionTimeout((Integer)mainParams.get("connectionTimeout"));

        // add datasource specific connection parameters
        if(configParams != null) configParams.forEach((k, v) -> ds.addDataSourceProperty(k, v));

        return ds;
    }


    /**
     * Get an instance of the datasource
     *
     * @return the DataSource object
     */
    public DataSource getDataSource() {
        return ds;
    }

}
