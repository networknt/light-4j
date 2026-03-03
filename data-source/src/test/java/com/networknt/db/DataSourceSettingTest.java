package com.networknt.db;

import com.networknt.service.SingletonServiceFactory;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DataSourceSettingTest {

    @Test
    public void testCustomMysqlDataSource() {
        CustomMysqlDataSource customMysqlDataSource = SingletonServiceFactory.getBean(CustomMysqlDataSource.class);
        HikariDataSource ds = customMysqlDataSource.getDataSource();
        assertEquals(ds.getIdleTimeout(), 50000);
        assertEquals(ds.getMinimumIdle(), 1);
        assertNotNull(ds);

    }

    @Test
    public void testMysqlDataSource() {
        MysqlDataSource customMysqlDataSource = SingletonServiceFactory.getBean(MysqlDataSource.class);
        HikariDataSource ds = customMysqlDataSource.getDataSource();
        assertEquals(ds.getIdleTimeout(), 600000);
        assertEquals(ds.getMinimumIdle(), -1);
        assertNotNull(ds);

    }
}
