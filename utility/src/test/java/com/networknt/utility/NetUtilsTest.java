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

/**
 * 
 */
package com.networknt.utility;

import org.junit.Assert;
import org.junit.Test;

import java.net.*;

/**
 * @author bozheng
 * 
 */
public class NetUtilsTest {

    @Test
    public void testGetLocalAddress() {
        InetAddress address = NetUtils.getLocalAddress();
        Assert.assertNotNull(address);
        Assert.assertTrue(NetUtils.isValidAddress(address));
        try {
            if(NetUtils.isValidAddress(InetAddress.getLocalHost())){
                Assert.assertEquals(InetAddress.getLocalHost(), address);
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetLocalAddressByDatagram() {
        String ip = NetUtils.getLocalAddressByDatagram();
        System.out.println("ip = " + ip);
    }

    @Test
    public void testResolveHost2ip() {
        String ip = NetUtils.resolveHost2Address("www.google.ca");
        System.out.println("ip = " + ip);
    }

    @Test
    public void testInetSocketAddress() throws UnknownHostException {
        InetSocketAddress inetSocketAddress1 = new InetSocketAddress("google.ca", 443);
        System.out.println("inetSocketAddress1 = " + inetSocketAddress1);
        InetAddress inetAddress = InetAddress.getByName("142.251.41.67");
        InetSocketAddress inetSocketAddress2 = new InetSocketAddress(inetAddress, 443);
        System.out.println("inetSocketAddress2 = " + inetSocketAddress2);
    }

    @Test
    public void testReplaceUriHostname2Address() throws URISyntaxException {
        URI uri = new URI("https://localhost:8443");
        uri = NetUtils.resolveUriHost2Address(uri);
        System.out.println("uri = " + uri);
    }
}
