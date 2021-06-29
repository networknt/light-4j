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

import java.util.Map;

/**
 * Sample customized the MysqlDataSource by adding extended config
 *
 * @author Gavin Chen
 */
public class CustomMysqlDataSource extends MysqlDataSource {

    private static final String CUSTOM_MYSQL_DS = "MysqlDataSource";
    private static final String LEAK_DETECTION_THRESHOLD = "leakDetectionThreshold";
    private static final String REGISTER_MBEANS = "registerMBeans";
    private static final String CONNECTION_TEST_QUERY = "testQuery";


    @Override
    public String getDsName() {
        if(dsName != null) {
            return dsName;
        } else {
            return CUSTOM_MYSQL_DS;
        }
    }

    public CustomMysqlDataSource(String dsName) {
        super(dsName);
        setExtendConfigParams();
    }

    public CustomMysqlDataSource() {
        super();
        setExtendConfigParams();
    }

    public void setExtendConfigParams() {
        Map<String, Object> mainParams = (Map<String, Object>) dataSourceMap.get(getDsName());
        if (mainParams.containsKey(LEAK_DETECTION_THRESHOLD))  this.getDataSource().setLeakDetectionThreshold((Integer)mainParams.get(LEAK_DETECTION_THRESHOLD));
        if (mainParams.containsKey(REGISTER_MBEANS))  this.getDataSource().setRegisterMbeans((Boolean) mainParams.get(REGISTER_MBEANS));
        if (mainParams.containsKey(CONNECTION_TEST_QUERY))  this.getDataSource().setConnectionTestQuery((String)mainParams.get(CONNECTION_TEST_QUERY));

    }

}
