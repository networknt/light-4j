package com.networknt.discovery;

import org.junit.Test;

import java.net.URL;
import java.util.List;

/**
 * Created by stevehu on 2016-12-07.
 */
public class LocalDiscoveryServiceTest {
    @Test
    public void testDoDiscovery() {
        LocalDiscoveryService service = new LocalDiscoveryService();
        List<URL> services = service.discover("A");
        System.out.println("services = " + services);
    }
}
