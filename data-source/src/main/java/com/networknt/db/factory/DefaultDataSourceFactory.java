package com.networknt.db.factory;


import com.networknt.config.Config;
import com.networknt.db.GenericDataSource;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

import java.util.*;

public class DefaultDataSourceFactory implements DataSourceFactory{

    // use light4j datasource config
    public static final String DATASOURCE = "datasource";
    public  static  final Map<String, DataSource> dataSources = Collections.synchronizedMap(new HashMap<>());
    public  static  final Map<String, Object> dataSourceMap = Config.getInstance().getJsonMapConfig(DATASOURCE);

    @Override
    public DataSource getDataSource(String name) {
        if (dataSources.containsKey(name)) {
            return dataSources.get(name);
        }
        GenericDataSource genericDataSource = new GenericDataSource(name);
        HikariDataSource hkDs = genericDataSource.getDataSource();
        DataSource result = hkDs;
        //Map<String, Object> mainParams = (Map<String, Object>) dataSourceMap.get(name);
        //String dsClazz = StringUtils.trimToEmpty((String)mainParams.getOrDefault("dataSourceClassName", mainParams.get("DataSourceClassName")));
        //TODO add XA datasource build
        dataSources.put(name, result);
        return result;
    }
}
