package com.networknt.db;

/**
 * Oracle database data source. We are using XE docker container for testing.
 *
 * @author Steve Hu
 */
public class OracleDataSource extends GenericDataSource {
    private static final String ORACLE_DS = "OracleDataSource";
    private static final String ORACLE_PASSWORD = "oraclePassword";

    @Override
    public String getDsName() {
        if(dsName != null) {
            return dsName;
        } else {
            return ORACLE_DS;
        }
    }

    @Override
    public String getDbPassKey() {
        return ORACLE_PASSWORD;
    }

    public OracleDataSource(String dsName) {
        super(dsName);
    }

    public OracleDataSource() {
        super();
    }

}
