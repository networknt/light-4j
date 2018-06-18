package com.networknt.db;

/**
 * The most populate open source database.
 *
 * @author Steve Hu
 */
public class MysqlDataSource extends GenericDataSource {
    private static final String MYSQL_DS = "MysqlDataSource";
    private static final String MYSQL_PASSWORD = "mysqlPassword";

    @Override
    public String getDsName() {
        if(dsName != null) {
            return dsName;
        } else {
            return MYSQL_DS;
        }
    }

    @Override
    public String getDbPassKey() {
        return MYSQL_PASSWORD;
    }

    public MysqlDataSource(String dsName) {
        super(dsName);
    }

    public MysqlDataSource() {
        super();
    }

}
