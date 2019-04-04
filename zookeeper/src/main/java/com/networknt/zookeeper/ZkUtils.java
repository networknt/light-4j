/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */


package com.networknt.zookeeper;

import com.networknt.status.Status;
import com.networknt.utility.Constants;
import com.networknt.exception.FrameworkException;
import com.networknt.registry.URL;

/**
 * ZooKeeper Utilities that contain some static method to help communicate with
 * the server.
 *
 * @author Steve Hu
 */
public class ZkUtils {
    private static final String GET_NODETYPEPATH_ERROR = "ERR10026";
    public static String toGroupPath(URL url) {
        return Constants.ZOOKEEPER_REGISTRY_NAMESPACE + Constants.PATH_SEPARATOR + url.getGroup();
    }

    public static String toServicePath(URL url) {
        return toGroupPath(url) + Constants.PATH_SEPARATOR + url.getPath();
    }

    public static String toCommandPath(URL url) {
        return toGroupPath(url) + Constants.ZOOKEEPER_REGISTRY_COMMAND;
    }

    public static String toNodeTypePath(URL url, ZkNodeType nodeType) {
        String type;
        if (nodeType == ZkNodeType.AVAILABLE_SERVER) {
            type = "server";
        } else if (nodeType == ZkNodeType.UNAVAILABLE_SERVER) {
            type = "unavailableServer";
        } else if (nodeType == ZkNodeType.CLIENT) {
            type = "client";
        } else {
            throw new FrameworkException(new Status(GET_NODETYPEPATH_ERROR, url, nodeType.toString()));
        }
        return toServicePath(url) + Constants.PATH_SEPARATOR + type;
    }

    public static String toNodePath(URL url, ZkNodeType nodeType) {
        return toNodeTypePath(url, nodeType) + Constants.PATH_SEPARATOR + url.getServerPortStr();
    }
}
