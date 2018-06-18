package com.networknt.db;

/**
 * H2 database data source. Usually it is used for testing and it can simulate other database by specify
 * the mode. It also load the script during the startup in the jdbcUrl. When used for test, we would use
 * the in-memory mode so that no filesystem is used.
 *
 * @author Steve Hu
 */
public class H2DataSource extends GenericDataSource {
    private static final String H2_DS = "H2DataSource";
    private static final String H2_PASSWORD = "H2Password";

    @Override
    public String getDsName() {
        if(dsName != null) {
            return dsName;
        } else {
            return H2_DS;
        }
    }

    @Override
    public String getDbPassKey() {
        return H2_PASSWORD;
    }

    public H2DataSource(String dsName) {
        super(dsName);
    }

    public H2DataSource() {
        super();
    }
}
