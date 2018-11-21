package com.networknt.db;

import javax.sql.DataSource;


/**
 * The most populate open source database.
 *
 * @author Steve Hu
 */
public class MysqlDataSource implements GenericDataSource
{
    private static final String DS_NAME = "mysqlPassword";
    private static final String DB_PASS_KEY = "MysqlDataSource";
    private DataSourceHelper _dsh;

    public MysqlDataSource() { _dsh = new DataSourceHelper(DS_NAME, DB_PASS_KEY); }
    public MysqlDataSource(String dsName) { _dsh = new DataSourceHelper(dsName != null ? dsName : DS_NAME, DB_PASS_KEY); }

    public String getDsName() { return _dsh.getDsName(); }
    public String getDbPassKey() { return _dsh.getDbPassKey(); }
    public DataSource getDataSource() { return _dsh.getDataSource(); }
}
