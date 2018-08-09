package com.networknt.consul;

import com.networknt.registry.URLImpl;
import com.networknt.utility.Constants;
import com.networknt.registry.URLParamType;
import com.networknt.registry.URL;
import com.networknt.utility.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConsulUtils {

    /**
     * Check if two lists have the same urls. If any list is empty, return false
     *
     * @param urls1 first url list
     * @param urls2 second url list
     * @return boolean true when they are the same
     */
    public static boolean isSame(List<URL> urls1, List<URL> urls2) {
        if (urls1 == null || urls2 == null) {
            return false;
        }
        if (urls1.size() != urls2.size()) {
            return false;
        }
        return urls1.containsAll(urls2);
    }

    /**
     * build consul service from url
     *
     * @param url a URL object
     * @return ConsulService consul service
     */
    public static ConsulService buildService(URL url) {
        ConsulService service = new ConsulService();
        service.setAddress(url.getHost());
        service.setId(ConsulUtils.convertConsulSerivceId(url));
        service.setName(url.getPath());
        service.setPort(url.getPort());
        List<String> tags = new ArrayList<String>();
        String env = url.getParameter(Constants.TAG_ENVIRONMENT);
        if(env != null) tags.add(env);
        service.setTags(tags);

        return service;
    }

    /**
     * build url from service
     *
     * @param service consul service
     * @return URL object
     */
    public static URL buildUrl(ConsulService service) {
        URL url = null;
        if (url == null) {
            Map<String, String> params = new HashMap<String, String>();
            //String group = service.getName();
            //params.put(URLParamType.group.getName(), group);
            //params.put(URLParamType.nodeType.getName(), Constants.NODE_TYPE_SERVICE);
            url = new URLImpl(ConsulConstants.DEFAULT_PROTOCOL, service.getAddress(), service.getPort(),
                    ConsulUtils.getPathFromServiceId(service.getId()), params);
        }
        return url;
    }

    /**
     * get cluster info from url, cluster info (protocol, path)
     *
     * @param url a URL object
     * @return String url cluster info
     */
    public static String getUrlClusterInfo(URL url) {
        return url.getPath();
    }

    /**
     * convert group to service name
     *
     * @param group group
     * @return String service name
     */
    public static String convertGroupToServiceName(String group) {
        return group;
    }

    /**
     * get group from consul service
     *
     * @param group group
     * @return group
     */
    public static String getGroupFromServiceName(String group) {
        return group;
    }

    /**
     * convert url to consul service id. serviceid includes ip＋port＋service
     *
     * @param url a URL object
     * @return service id
     */
    public static String convertConsulSerivceId(URL url) {
        if (url == null) {
            return null;
        }
        return convertServiceId(url.getHost(), url.getPort(), url.getPath());
    }

    /**
     * get path of url from service id in consul
     *
     * @param serviceId service id
     * @return path
     */
    public static String getPathFromServiceId(String serviceId) {
        return serviceId.substring(serviceId.indexOf(":") + 1, serviceId.lastIndexOf(":"));
    }

    /**
     * get protocol from consul tag
     *
     * @param tag tag
     * @return protocol
     */
    public static String getProtocolFromTag(String tag) {
        return tag.substring(ConsulConstants.CONSUL_TAG_LIGHT_PROTOCOL.length());
    }


    public static String convertServiceId(String host, int port, String path) {
        return host + ":" + path + ":" + port;
    }

}
