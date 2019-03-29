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
 * H2 database data source. Usually it is used for testing and it can simulate other database by specify
 * the mode. It also load the script during the startup in the jdbcUrl. When used for test, we would use
 * the in-memory mode so that no filesystem is used.
 *
 * @author Steve Hu
 */
public class H2DataSource extends GenericDataSource {
    private static final String H2_DS = "H2DataSource";
    private static final String H2_PASSWORD = "H2Password";

    @Override
    public String getDsName() {
        if(dsName != null) {
            return dsName;
        } else {
            return H2_DS;
        }
    }

    @Override
    public String getDbPassKey() {
        return H2_PASSWORD;
    }

    public H2DataSource(String dsName) {
        super(dsName);
    }

    public H2DataSource() {
        super();
    }
}
