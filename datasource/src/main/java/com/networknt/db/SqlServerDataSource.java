package com.networknt.db;

/**
 * Microsoft SQL Server database data source.
 *
 * @author Steve Hu
 */
public class SqlServerDataSource extends GenericDataSource {
    private static final String SQLSERVER_DS = "SqlServerDataSource";
    private static final String SQLSERVER_PASSWORD = "sqlServerPassword";

    @Override
    public String getDsName() {
        if(dsName != null) {
            return dsName;
        } else {
            return SQLSERVER_DS;
        }
    }

    @Override
    public String getDbPassKey() {
        return SQLSERVER_PASSWORD;
    }

    public SqlServerDataSource(String dsName) {
        super(dsName);
    }

    public SqlServerDataSource() {
        super();
    }

}
