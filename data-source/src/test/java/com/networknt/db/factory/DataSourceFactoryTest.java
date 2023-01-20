package com.networknt.db.factory;


import com.networknt.service.SingletonServiceFactory;
import org.junit.Test;

import javax.sql.DataSource;


import static org.junit.Assert.assertNotNull;

public class DataSourceFactoryTest {

    @Test
    public void testDataSource() {
        DataSource dataSource1 = SingletonServiceFactory.getBean(DataSourceFactory.class).getDataSource("OracleDataSource");
        DataSource dataSource2 = SingletonServiceFactory.getBean(DataSourceFactory.class).getDataSource("OracleDataSource2");
        assertNotNull(dataSource1);
        assertNotNull(dataSource2);

    }


}
