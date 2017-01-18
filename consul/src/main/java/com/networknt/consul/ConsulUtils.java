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
        service.setName(ConsulUtils.convertGroupToServiceName(url.getGroup()));
        service.setPort(url.getPort());
        service.setTtl(ConsulConstants.TTL);

        List<String> tags = new ArrayList<String>();
        tags.add(ConsulConstants.CONSUL_TAG_LIGHT_PROTOCOL + url.getProtocol());
        tags.add(ConsulConstants.CONSUL_TAG_LIGHT_URL + Util.urlEncode(url.toFullStr()));
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
        for (String tag : service.getTags()) {
            if (tag.startsWith(ConsulConstants.CONSUL_TAG_LIGHT_URL)) {
                String encodeUrl = tag.substring(tag.indexOf("_") + 1);
                url = URLImpl.valueOf(Util.urlDecode(encodeUrl));
            }
        }
        if (url == null) {
            Map<String, String> params = new HashMap<String, String>();
            String group = service.getName().substring(ConsulConstants.CONSUL_SERVICE_LIGHT_PRE.length());
            params.put(URLParamType.group.getName(), group);
            params.put(URLParamType.nodeType.getName(), Constants.NODE_TYPE_SERVICE);
            String protocol = ConsulUtils.getProtocolFromTag(service.getTags().get(0));
            url = new URLImpl(protocol, service.getAddress(), service.getPort(),
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
        return url.getProtocol() + "-" + url.getPath();
    }

    /**
     * convert group to service name
     *
     * @param group group
     * @return String service name
     */
    public static String convertGroupToServiceName(String group) {
        return ConsulConstants.CONSUL_SERVICE_LIGHT_PRE + group;
    }

    /**
     * get group from consul service
     *
     * @param group group
     * @return group
     */
    public static String getGroupFromServiceName(String group) {
        return group.substring(ConsulConstants.CONSUL_SERVICE_LIGHT_PRE.length());
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
        return serviceId.substring(serviceId.indexOf("-") + 1);
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
        return host + ":" + port + "-" + path;
    }

}
