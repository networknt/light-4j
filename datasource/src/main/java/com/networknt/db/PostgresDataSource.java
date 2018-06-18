package com.networknt.db;

/**
 * Another popular open source database.
 *
 * @author Steve Hu
 */
public class PostgresDataSource extends GenericDataSource {
    private static final String POSTGRES_DS = "PostgresDataSource";
    private static final String POSTGRES_PASSWORD = "postgresPassword";

    @Override
    public String getDsName() {
        if(dsName != null) {
            return dsName;
        } else {
            return POSTGRES_DS;
        }
    }

    @Override
    public String getDbPassKey() {
        return POSTGRES_PASSWORD;
    }

    public PostgresDataSource(String dsName) {
        super(dsName);
    }

    public PostgresDataSource() {
        super();
    }

}
