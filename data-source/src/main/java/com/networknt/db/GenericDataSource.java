package com.networknt.db;

import javax.sql.DataSource;

public interface GenericDataSource {
    String getDsName();

    String getDbPassKey();

    DataSource getDataSource();
}
