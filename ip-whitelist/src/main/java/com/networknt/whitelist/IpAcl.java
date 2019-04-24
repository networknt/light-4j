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

package com.networknt.whitelist;

import java.util.ArrayList;
import java.util.List;

public class IpAcl {
    private List<WhitelistHandler.PeerMatch> ipv6acl = new ArrayList<>();
    private List<WhitelistHandler.PeerMatch> ipv4acl = new ArrayList<>();

    public List<WhitelistHandler.PeerMatch> getIpv6acl() {
        return ipv6acl;
    }

    public void setIpv6acl(List<WhitelistHandler.PeerMatch> ipv6acl) {
        this.ipv6acl = ipv6acl;
    }

    public List<WhitelistHandler.PeerMatch> getIpv4acl() {
        return ipv4acl;
    }

    public void setIpv4acl(List<WhitelistHandler.PeerMatch> ipv4acl) {
        this.ipv4acl = ipv4acl;
    }
}
