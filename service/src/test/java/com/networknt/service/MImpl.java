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

package com.networknt.service;

/**
 * Created by stevehu on 2017-01-18.
 */
public class MImpl implements M {
    String name;
    int value = 0;

    public MImpl(String name, int v1, int v2) {
        this.name = name;
        value = v1 + v2;
    }
    @Override
    public int getValue() {
        return value;
    }
}
