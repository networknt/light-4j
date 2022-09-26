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
package com.networknt.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author fishermen
 * @version V1.0 created at: 2013-5-28
 */
public class NetUtils {
    private static final Logger logger = LoggerFactory.getLogger(NetUtils.class);

    public static final String LOCALHOST = "127.0.0.1";

    public static final String ANYHOST = "0.0.0.0";

    private static volatile InetAddress LOCAL_ADDRESS = null;

    private static final Pattern LOCAL_IP_PATTERN = Pattern.compile("127(\\.\\d{1,3}){3}$");

    private static final Pattern ADDRESS_PATTERN = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}\\:\\d{1,5}$");

    private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");

    public static boolean isInvalidLocalHost(String host) {
        return host == null || host.length() == 0 || host.equalsIgnoreCase("localhost") || host.equals("0.0.0.0")
                || (LOCAL_IP_PATTERN.matcher(host).matches());
    }

    public static boolean isValidLocalHost(String host) {
        return !isInvalidLocalHost(host);
    }

    /**
     * {@link #getLocalAddress(Map)}
     *
     * @return InetAddress InetAddress
     */
    public static InetAddress getLocalAddress() {
        return getLocalAddress(null);
    }

    /**
     * <pre>
     * 1. check if you already have ip
     * 2. get ip from hostname
     * 3. get ip from socket
     * 4. get ip from network interface
     * </pre>
     * @param destHostPorts a map of ports
     * @return local ip
     */
    public static InetAddress getLocalAddress(Map<String, Integer> destHostPorts) {
        if (LOCAL_ADDRESS != null) {
            return LOCAL_ADDRESS;
        }

        InetAddress localAddress = getLocalAddressByHostname();
        if (!isValidAddress(localAddress)) {
            localAddress = getLocalAddressBySocket(destHostPorts);
        }

        if (!isValidAddress(localAddress)) {
            localAddress = getLocalAddressByNetworkInterface();
        }

        if (isValidAddress(localAddress)) {
            LOCAL_ADDRESS = localAddress;
        }

        return localAddress;
    }

    private static InetAddress getLocalAddressByHostname() {
        try {
            InetAddress localAddress = InetAddress.getLocalHost();
            if (isValidAddress(localAddress)) {
                return localAddress;
            }
        } catch (Throwable e) {
            logger.error("Failed to retriving local address by hostname:" + e);
        }
        return null;
    }

    private static InetAddress getLocalAddressBySocket(Map<String, Integer> destHostPorts) {
        if (destHostPorts == null || destHostPorts.size() == 0) {
            return null;
        }

        for (Map.Entry<String, Integer> entry : destHostPorts.entrySet()) {
            String host = entry.getKey();
            int port = entry.getValue();
            try {
                Socket socket = new Socket();
                try {
                    SocketAddress addr = new InetSocketAddress(host, port);
                    socket.connect(addr, 1000);
                    return socket.getLocalAddress();
                } finally {
                    try {
                        socket.close();
                    } catch (Throwable e) {}
                }
            } catch (Exception e) {
                logger.error(String.format("Failed to retriving local address by connecting to dest host:port(%s:%s) false, e=%s", host,
                        port, e));
            }
        }
        return null;
    }

    private static InetAddress getLocalAddressByNetworkInterface() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    try {
                        NetworkInterface network = interfaces.nextElement();
                        Enumeration<InetAddress> addresses = network.getInetAddresses();
                        while (addresses.hasMoreElements()) {
                            try {
                                InetAddress address = addresses.nextElement();
                                if (isValidAddress(address)) {
                                    return address;
                                }
                            } catch (Throwable e) {
                                logger.error("Failed to retriving ip address, " + e.getMessage(), e);
                            }
                        }
                    } catch (Throwable e) {
                        logger.error("Failed to retriving ip address, " + e.getMessage(), e);
                    }
                }
            }
        } catch (Throwable e) {
            logger.error("Failed to retriving ip address, " + e.getMessage(), e);
        }
        return null;
    }

    public static boolean isValidAddress(String address) {
        return ADDRESS_PATTERN.matcher(address).matches();
    }

    public static boolean isValidAddress(InetAddress address) {
        if (address == null || address.isLoopbackAddress()) return false;
        String name = address.getHostAddress();
        return (name != null && !ANYHOST.equals(name) && !LOCALHOST.equals(name) && IP_PATTERN.matcher(name).matches());
    }
    //return ip to avoid lookup dns
    public static String getHostName(SocketAddress socketAddress) {
        if (socketAddress == null) {
            return null;
        }

        if (socketAddress instanceof InetSocketAddress) {
            InetAddress addr = ((InetSocketAddress) socketAddress).getAddress();
            if(addr != null){
                return addr.getHostAddress();
            }
        }

        return null;
    }

    public static String getLocalAddressByDatagram() {
        try(final DatagramSocket socket = new DatagramSocket()){
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            return socket.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            logger.error("Failed to retrieving ip address.", e);
        }
        return null;
    }

    /**
     * Find a non-occupied port.
     *
     * @return A non-occupied port.
     */
    public static int getAvailablePort() {
        for (int i = 0; i < 50; i++) {
            try (ServerSocket serverSocket = new ServerSocket(0)) {
                int port = serverSocket.getLocalPort();
                if (port != 0) {
                    return port;
                }
            }
            catch (IOException ignored) {}
        }

        throw new RuntimeException("Could not find a free permitted port on the machine.");
    }

    /**
     * Normalizes and encodes a hostname and port to be included in URL.
     * In particular, this method makes sure that IPv6 address literals have the proper
     * formatting to be included in URLs.
     *
     * @param host The address to be included in the URL.
     * @param port The port for the URL address.
     * @return The proper URL string encoded IP address and port.
     * @throws java.net.UnknownHostException Thrown, if the hostname cannot be translated into a URL.
     */
    public static String hostAndPortToUrlString(String host, int port) throws UnknownHostException {
        return ipAddressAndPortToUrlString(InetAddress.getByName(host), port);
    }

    /**
     * Encodes an IP address and port to be included in URL. in particular, this method makes
     * sure that IPv6 addresses have the proper formatting to be included in URLs.
     *
     * @param address The address to be included in the URL.
     * @param port The port for the URL address.
     * @return The proper URL string encoded IP address and port.
     */
    public static String ipAddressAndPortToUrlString(InetAddress address, int port) {
        return ipAddressToUrlString(address) + ':' + port;
    }

    /**
     * Encodes an IP address properly as a URL string. This method makes sure that IPv6 addresses
     * have the proper formatting to be included in URLs.
     *
     * @param address The IP address to encode.
     * @return The proper URL string encoded IP address.
     */
    public static String ipAddressToUrlString(InetAddress address) {
        if (address == null) {
            throw new NullPointerException("address is null");
        }
        else if (address instanceof Inet4Address) {
            return address.getHostAddress();
        }
        else if (address instanceof Inet6Address) {
            return getIPv6UrlRepresentation((Inet6Address) address);
        }
        else {
            throw new IllegalArgumentException("Unrecognized type of InetAddress: " + address);
        }
    }

    /**
     * Creates a compressed URL style representation of an Inet6Address.
     *
     * <p>This method copies and adopts code from Google's Guava library.
     * We re-implement this here in order to reduce dependency on Guava.
     * The Guava library has frequently caused dependency conflicts in the past.
     */
    private static String getIPv6UrlRepresentation(Inet6Address address) {
        return getIPv6UrlRepresentation(address.getAddress());
    }

    /**
     * Creates a compressed URL style representation of an Inet6Address.
     *
     * <p>This method copies and adopts code from Google's Guava library.
     * We re-implement this here in order to reduce dependency on Guava.
     * The Guava library has frequently caused dependency conflicts in the past.
     */
    private static String getIPv6UrlRepresentation(byte[] addressBytes) {
        // first, convert bytes to 16 bit chunks
        int[] hextets = new int[8];
        for (int i = 0; i < hextets.length; i++) {
            hextets[i] = (addressBytes[2 * i] & 0xFF) << 8 | (addressBytes[2 * i + 1] & 0xFF);
        }

        // now, find the sequence of zeros that should be compressed
        int bestRunStart = -1;
        int bestRunLength = -1;
        int runStart = -1;
        for (int i = 0; i < hextets.length + 1; i++) {
            if (i < hextets.length && hextets[i] == 0) {
                if (runStart < 0) {
                    runStart = i;
                }
            } else if (runStart >= 0) {
                int runLength = i - runStart;
                if (runLength > bestRunLength) {
                    bestRunStart = runStart;
                    bestRunLength = runLength;
                }
                runStart = -1;
            }
        }
        if (bestRunLength >= 2) {
            Arrays.fill(hextets, bestRunStart, bestRunStart + bestRunLength, -1);
        }

        // convert into text form
        StringBuilder buf = new StringBuilder(40);
        buf.append('[');

        boolean lastWasNumber = false;
        for (int i = 0; i < hextets.length; i++) {
            boolean thisIsNumber = hextets[i] >= 0;
            if (thisIsNumber) {
                if (lastWasNumber) {
                    buf.append(':');
                }
                buf.append(Integer.toHexString(hextets[i]));
            } else {
                if (i == 0 || lastWasNumber) {
                    buf.append("::");
                }
            }
            lastWasNumber = thisIsNumber;
        }
        buf.append(']');
        return buf.toString();
    }

    public static String resolveHost2Address(String fqdn) {
        String ip = null;
        try {
            InetAddress address = InetAddress.getByName(fqdn);
            ip = address.getHostAddress();
        } catch (UnknownHostException e) {
            logger.error("UnknownHostException " + fqdn, e);
        }
        return ip;
    }

    public static URI resolveUriHost2Address(URI uri) {
        // convert the uri to URL.
        try {
            URL url = uri.toURL();
            String host = url.getHost();
            String ip = resolveHost2Address(host);
            if(ip != null) {
                url = new URL(url.getProtocol(), ip, url.getPort(), url.getFile());
                uri = url.toURI();
            }
        } catch (MalformedURLException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
        return uri;
    }
}
