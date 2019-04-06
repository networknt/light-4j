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

/**
 * Microsoft SQL Server database data source.
 *
 * @author Steve Hu
 */
public class SqlServerDataSource extends GenericDataSource {
    private static final String SQLSERVER_DS = "SqlServerDataSource";
    private static final String SQLSERVER_PASSWORD = "sqlServerPassword";

    @Override
    public String getDsName() {
        if(dsName != null) {
            return dsName;
        } else {
            return SQLSERVER_DS;
        }
    }

    @Override
    public String getDbPassKey() {
        return SQLSERVER_PASSWORD;
    }

    public SqlServerDataSource(String dsName) {
        super(dsName);
    }

    public SqlServerDataSource() {
        super();
    }

}
