package com.networknt.db;

import javax.sql.DataSource;

/**
 * H2 database data source. Usually it is used for testing and it can simulate other database by specify
 * the mode. It also load the script during the startup in the jdbcUrl. When used for test, we would use
 * the in-memory mode so that no filesystem is used.
 *
 * @author Steve Hu
 */
public class H2DataSource implements GenericDataSource
{
    private static final String DS_NAME = "H2DataSource";
    private static final String DB_PASS_KEY = "H2Password";

    private DataSourceHelper _dsh;

    public H2DataSource() { _dsh = new DataSourceHelper(DS_NAME, DB_PASS_KEY); }
    public H2DataSource(String dsName) { _dsh = new DataSourceHelper(dsName != null ? dsName : DS_NAME, DB_PASS_KEY); }

    public String getDsName() { return _dsh.getDsName(); }
    public String getDbPassKey() { return _dsh.getDbPassKey(); }
    public DataSource getDataSource() { return _dsh.getDataSource(); }
}
