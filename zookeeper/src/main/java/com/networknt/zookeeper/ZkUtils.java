package com.networknt.zookeeper;

import com.networknt.status.Status;
import com.networknt.utility.Constants;
import com.networknt.status.exception.FrameworkException;
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
