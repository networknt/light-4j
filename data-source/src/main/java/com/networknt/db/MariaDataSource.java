package com.networknt.db;

/**
 * Maria DB data source that is a drop-in replacement for Mysql database.
 *
 * @author Steve Hu
 */
public class MariaDataSource extends GenericDataSource {
    private static final String MARIA_DS = "MariaDataSource";
    private static final String MARIA_PASSWORD = "mariaPassword";

    @Override
    public String getDsName() {
        if(dsName != null) {
            return dsName;
        } else {
            return MARIA_DS;
        }
    }

    @Override
    public String getDbPassKey() {
        return MARIA_PASSWORD;
    }

    public MariaDataSource(String dsName) {
        super(dsName);
    }

    public MariaDataSource() {
        super();
    }

}
