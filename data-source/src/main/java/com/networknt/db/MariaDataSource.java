package com.networknt.db;

import javax.sql.DataSource;

/**
 * Maria DB data source that is a drop-in replacement for Mysql database.
 *
 * @author Steve Hu
 */
public class MariaDataSource implements GenericDataSource
{
    private static final String DS_NAME = "MariaDataSource";
    private static final String DB_PASS_KEY = "mariaPassword";

    private DataSourceHelper _dsh;

    public MariaDataSource() { _dsh = new DataSourceHelper(DS_NAME, DB_PASS_KEY); }
    public MariaDataSource(String dsName) { _dsh = new DataSourceHelper(dsName != null ? dsName : DS_NAME, DB_PASS_KEY); }

    public String getDsName() { return _dsh.getDsName(); }
    public String getDbPassKey() { return _dsh.getDbPassKey(); }
    public DataSource getDataSource() { return _dsh.getDataSource(); }
}
