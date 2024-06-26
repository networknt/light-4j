/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.db;

import com.networknt.config.Config;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * This is the generic Hikari datasource and it is suitable if you only have one database
 * in your application. You can still use it when you have one datasource for production
 * and another one for testing by putting two different version of service.yml and datasource.yml
 * in both main/resources/config and test/resources/config.
 *
 * @author Steve Hu
 */
public class GenericDataSource {
    public static final String DATASOURCE = "datasource";
    public static final String DB_PASSWORD = "password";
    public static final String DS_NAME = "H2DataSource";
    public static final String PARAMETERS = "parameters";
    public static final String SETTINGS = "settings";
    public static final String JDBC_URL = "jdbcUrl";
    public static final String USERNAME = "username";
    public static final String MAXIMUM_POOL_SIZE = "maximumPoolSize";
    public static final String CONNECTION_TIMEOUT = "connectionTimeout";

    private static final Logger logger = LoggerFactory.getLogger(GenericDataSource.class);

    // the HikariDataSource
    private HikariDataSource ds;
    // the data source name
    protected String dsName;
    protected Map<String, Object> dataSourceMap;

    public String getDsName() {
        return dsName;
    }

    public String getDbPassKey() {
        return DB_PASSWORD;
    }

    public GenericDataSource() {
        this.dsName = DS_NAME;
        this.ds = createDataSource();
    }

    public GenericDataSource(String dsName) {
        this.dsName = dsName;
        this.ds = createDataSource();
    }

    protected HikariDataSource createDataSource() {
        // get the configured datasources
        dataSourceMap = Config.getInstance().getJsonMapConfig(DATASOURCE);
        // get the requested datasource
        Map<String, Object> mainParams = (Map<String, Object>) dataSourceMap.get(getDsName());
        Map<String, String> configParams = (Map<String, String>)mainParams.get(PARAMETERS);
        Map<String, Object> settings = (Map<String, Object>)mainParams.get(SETTINGS);

        // create the DataSource
        ds = new HikariDataSource();
        ds.setJdbcUrl((String)mainParams.get(JDBC_URL));
        ds.setUsername((String)mainParams.get(USERNAME));

        // use encrypted password
        String password = (String)mainParams.get(DB_PASSWORD);
        ds.setPassword(password);

        // set datasource paramters
        ds.setMaximumPoolSize(Config.loadIntegerValue(MAXIMUM_POOL_SIZE, mainParams.get(MAXIMUM_POOL_SIZE)));
        ds.setConnectionTimeout(Config.loadIntegerValue(CONNECTION_TIMEOUT, mainParams.get(CONNECTION_TIMEOUT)));

        if (settings != null && settings.size()>0) {
            for (Map.Entry<String, Object> entry: settings.entrySet()) {
                String fieldName = entry.getKey();
                try {
                    Method method =  findMethod(ds.getClass(), "set" + convertFirstLetterUpper(fieldName), entry.getValue().getClass());
                    method.invoke(ds, entry.getValue());
                } catch (Exception e) {
                    logger.error("no such set method on datasource for setting value:" + fieldName);
                }
            }
        }

        // add datasource specific connection parameters
        if(configParams != null) configParams.forEach((k, v) -> ds.addDataSourceProperty(k, v));

        return ds;
    }

    private String convertFirstLetterUpper(String field) {
        return field.substring(0,1).toUpperCase() + field.substring(1);
    }

    /**
     * Get an instance of the datasource
     *
     * @return the HikariDataSource object
     */
    public HikariDataSource getDataSource() {
        return ds;
    }

    public  Method findMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {

        try {
            return clazz.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException ex) {
        }

        // Then loop through all available methods, checking them one by one.
        for (Method method : clazz.getMethods()) {

            String name = method.getName();
            if (!methodName.equals(name)) { // The method must have right name.
                continue;
            }

            Class<?>[] acceptedParameterTypes = method.getParameterTypes();
            if (acceptedParameterTypes.length != parameterTypes.length) { // Must have right number of parameters.
                continue;
            }

            //For some special cases, we may need add type cast or class isAssignableFrom here to verify method
            return method;
        }

        // None of our trials was successful!
        throw new NoSuchMethodException();
    }
}
