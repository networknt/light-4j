package com.networknt.db;

import javax.sql.DataSource;

/**
 * Microsoft SQL Server database data source.
 *
 * @author Steve Hu
 */
public class SqlServerDataSource implements GenericDataSource
{
    private static final String DS_NAME = "SqlServerDataSource";
    private static final String DB_PASS_KEY = "sqlServerPassword";
    private DataSourceHelper _dsh;

    public SqlServerDataSource() { _dsh = new DataSourceHelper(DS_NAME, DB_PASS_KEY); }
    public SqlServerDataSource(String dsName) { _dsh = new DataSourceHelper(dsName != null ? dsName : DS_NAME, DB_PASS_KEY); }

    public String getDsName() { return _dsh.getDsName(); }
    public String getDbPassKey() { return _dsh.getDbPassKey(); }
    public DataSource getDataSource() { return _dsh.getDataSource(); }
}
