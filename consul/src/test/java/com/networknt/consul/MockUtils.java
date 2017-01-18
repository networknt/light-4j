package com.networknt.consul;

import com.networknt.registry.URLImpl;
import com.networknt.registry.URLParamType;
import com.networknt.registry.URL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhanglei28
 * @Description MockUtils
 */
public class MockUtils {
    //  mock service info
    private static String path = "mockService";
    private static String group = "mockGroup";
    private static String address = "127.0.0.1";
    private static String protocol = "light";

    public static ConsulService getMockService(int id) {

        ConsulService service = new ConsulService();
        service.setAddress(address);
        service.setId(ConsulUtils.convertServiceId(address, id, path));
        service.setName(ConsulUtils.convertGroupToServiceName(group));
        service.setPort(id);
        List<String> tags = new ArrayList<String>();
        tags.add(ConsulConstants.CONSUL_TAG_LIGHT_PROTOCOL + ":" + protocol);
        service.setTags(tags);

        return service;
    }

    /**
     * get mock url, use it to query mock service
     *
     * @return URL a URL object
     */
    public static URL getMockUrl(int port) {
        return getMockUrl(address, port);
    }

    public static URL getMockUrl(String address, int port) {
        Map<String, String> params = new HashMap<>();
        params.put(URLParamType.group.getName(), group);
        params.put(URLParamType.protocol.getName(), protocol);
        URL url = new URLImpl(protocol, address, port, path, params);
        return url;
    }

}
