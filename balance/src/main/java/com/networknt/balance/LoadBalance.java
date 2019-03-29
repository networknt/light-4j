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

package com.networknt.balance;

import com.networknt.registry.URL;

import java.util.List;

public interface LoadBalance {
    // select one from a list of URLs

    /**
     * Select one url from a list of url with requestKey as optional.
     *
     * @param urls List
     * @param requestKey String
     * @return URL
     */
    URL select(List<URL> urls, String requestKey);

    /**
     * return positive int value of originValue
     * @param originValue original value
     * @return positive int
     */
    default int getPositive(int originValue){
        return 0x7fffffff & originValue;
    }

}
