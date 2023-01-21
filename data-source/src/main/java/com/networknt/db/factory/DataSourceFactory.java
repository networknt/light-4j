package com.networknt.db.factory;

import javax.sql.DataSource;

public interface DataSourceFactory {
    DataSource getDataSource(String name);
}
