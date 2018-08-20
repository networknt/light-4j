package com.networknt.db;

import com.networknt.service.SingletonServiceFactory;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Ignore;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.assertNotNull;

public class GenericDataSourceTest {

    @Test
    public void testGetDataSource() {
        DataSource ds = SingletonServiceFactory.getBean(DataSource.class);
        assertNotNull(ds);

        HikariDataSource hds = (HikariDataSource)ds;
        System.out.println(hds.getMaximumPoolSize());

        try(Connection connection = ds.getConnection()){
            assertNotNull(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        hds = (HikariDataSource)ds;
        System.out.println(hds.getMaximumPoolSize());
    }

    @Test
    public void testGetH2DataSource() {
        DataSource ds = SingletonServiceFactory.getBean(H2DataSource.class).getDataSource();
        assertNotNull(ds);

        try(Connection connection = ds.getConnection()){
            assertNotNull(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetMysqlDataSource() {
        DataSource ds = SingletonServiceFactory.getBean(MysqlDataSource.class).getDataSource();
        assertNotNull(ds);

        try(Connection connection = ds.getConnection()){
            assertNotNull(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    @Ignore
    public void testGetMariaDataSource() {
        DataSource ds = SingletonServiceFactory.getBean(MariaDataSource.class).getDataSource();
        assertNotNull(ds);

        try(Connection connection = ds.getConnection()){
            assertNotNull(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    @Ignore
    public void testGetPostgresDataSource() {
        DataSource ds = SingletonServiceFactory.getBean(PostgresDataSource.class).getDataSource();
        assertNotNull(ds);

        try(Connection connection = ds.getConnection()){
            assertNotNull(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    @Ignore
    public void testGetSqlServerDataSource() {
        DataSource ds = SingletonServiceFactory.getBean(SqlServerDataSource.class).getDataSource();
        assertNotNull(ds);

        try(Connection connection = ds.getConnection()){
            assertNotNull(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    @Ignore
    public void testGetOracleDataSource() {
        DataSource ds = SingletonServiceFactory.getBean(OracleDataSource.class).getDataSource();
        assertNotNull(ds);

        try(Connection connection = ds.getConnection()){
            assertNotNull(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}

