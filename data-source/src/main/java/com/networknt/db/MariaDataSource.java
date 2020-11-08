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
 * Maria DB data source that is a drop-in replacement for Mysql database.
 *
 * @author Steve Hu
 */
public class MariaDataSource extends GenericDataSource {
    private static final String MARIA_DS = "MariaDataSource";

    @Override
    public String getDsName() {
        if(dsName != null) {
            return dsName;
        } else {
            return MARIA_DS;
        }
    }

    public MariaDataSource(String dsName) {
        super(dsName);
    }

    public MariaDataSource() {
        super();
    }

}
