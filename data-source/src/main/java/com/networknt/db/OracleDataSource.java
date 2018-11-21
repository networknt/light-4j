package com.networknt.db;

import javax.sql.DataSource;

/**
 * Oracle database data source. We are using XE docker container for testing.
 *
 * @author Steve Hu
 */
public class OracleDataSource implements GenericDataSource
{
    private static final String DS_NAME = "OracleDataSource";
    private static final String DB_PASS_KEY = "oraclePassword";
    private DataSourceHelper _dsh;

    public OracleDataSource() { _dsh = new DataSourceHelper(DS_NAME, DB_PASS_KEY); }
    public OracleDataSource(String dsName) { _dsh = new DataSourceHelper(dsName != null ? dsName : DS_NAME, DB_PASS_KEY); }

    public String getDsName() { return _dsh.getDsName(); }
    public String getDbPassKey() { return _dsh.getDbPassKey(); }
    public DataSource getDataSource() { return _dsh.getDataSource(); }
}
