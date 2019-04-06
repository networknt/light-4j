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

package com.networknt.basicauth;

import com.networknt.common.DecryptUtil;

import java.util.List;
import java.util.Map;

public class BasicAuthConfig {
    boolean enabled;
    List<Map<String, Object>> users;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<Map<String, Object>> getUsers() { return users; }
    public void setUsers(List<Map<String, Object>> users) {
        users.forEach(user -> DecryptUtil.decryptMap(user));
        this.users = users;
    }
}
