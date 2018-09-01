package com.networknt.registry;

import com.networknt.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of URL interface.
 *
 * @author Steve Hu
 */
public class URLImpl implements URL {
    private static final Logger logger = LoggerFactory.getLogger(URLImpl.class);

    private String protocol;

    private String host;

    private int port;

    // interfaceName
    private String path;

    private Map<String, String> parameters;

    private volatile transient Map<String, Number> numbers;

    public URLImpl(String protocol, String host, int port, String path) {
        this(protocol, host, port, path, new HashMap<>());
    }
    public URLImpl() {

    }

    public URLImpl(String protocol, String host, int port, String path, Map<String, String> parameters) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.path = path;
        this.parameters = parameters;
    }

    public static URL valueOf(String url) {

        String protocol = null;
        String host = null;
        int port = 0;
        String path = null;
        Map<String, String> parameters = new HashMap<>();
        int i = url.indexOf("?"); // separator between body and parameters
        if (i >= 0) {
            String[] parts = url.substring(i + 1).split("&");

            for (String part : parts) {
                part = part.trim();
                if (part.length() > 0) {
                    int j = part.indexOf('=');
                    if (j >= 0) {
                        parameters.put(part.substring(0, j), part.substring(j + 1));
                    } else {
                        parameters.put(part, part);
                    }
                }
            }
            url = url.substring(0, i);
        }
        i = url.indexOf("://");
        if (i >= 0) {
            if (i == 0) throw new IllegalStateException("url missing protocol: \"" + url + "\"");
            protocol = url.substring(0, i);
            url = url.substring(i + 3);
        } else {
            i = url.indexOf(":/");
            if (i >= 0) {
                if (i == 0) throw new IllegalStateException("url missing protocol: \"" + url + "\"");
                protocol = url.substring(0, i);
                url = url.substring(i + 1);
            }
        }

        i = url.indexOf("/");
        if (i >= 0) {
            path = url.substring(i + 1);
            url = url.substring(0, i);
        }

        i = url.indexOf(":");
        if (i >= 0 && i < url.length() - 1) {
            port = Integer.parseInt(url.substring(i + 1));
            url = url.substring(0, i);
        }
        if (url.length() > 0) host = url;
        if(port == 0) {
            // set the default port based on the protocol
            if("http".equalsIgnoreCase(protocol)) {
                port = 80;
            } else if("https".equalsIgnoreCase(protocol)) {
                port = 443;
            } else {
                logger.error("Unknown protocol " + protocol);
            }
        }
        return new URLImpl(protocol, host, port, path, parameters);
    }

    private static String buildHostPortStr(String host, int defaultPort) {
        if (defaultPort <= 0) {
            return host;
        }

        int idx = host.indexOf(":");
        if (idx < 0) {
            return host + ":" + defaultPort;
        }

        int port = Integer.parseInt(host.substring(idx + 1));
        if (port <= 0) {
            return host.substring(0, idx + 1) + defaultPort;
        }
        return host;
    }

    @Override
    public URL createCopy() {
        Map<String, String> params = new HashMap<>();
        if (this.parameters != null) {
            params.putAll(this.parameters);
        }

        return new URLImpl(protocol, host, port, path, params);
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public Integer getPort() {
        return port;
    }

    @Override
    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String getVersion() {
        return getParameter(URLParamType.version.getName(), URLParamType.version.getValue());
    }

    @Override
    public String getGroup() {
        return getParameter(URLParamType.group.getName(), URLParamType.group.getValue());
    }

    @Override
    public Map<String, String> getParameters() {
        return parameters;
    }

    @Override
    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    @Override
    public String getParameter(String name) {
        return parameters.get(name);
    }

    @Override
    public String getParameter(String name, String defaultValue) {
        String value = getParameter(name);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    @Override
    public String getMethodParameter(String methodName, String paramDesc, String name) {
        String value = getParameter(Constants.METHOD_CONFIG_PREFIX + methodName + "(" + paramDesc + ")." + name);
        if (value == null || value.length() == 0) {
            return getParameter(name);
        }
        return value;
    }

    @Override
    public String getMethodParameter(String methodName, String paramDesc, String name, String defaultValue) {
        String value = getMethodParameter(methodName, paramDesc, name);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return value;
    }

    @Override
    public void addParameter(String name, String value) {
        if (name == null || value == null) {
            return;
        }
        parameters.put(name, value);
    }

    @Override
    public void removeParameter(String name) {
        if (name != null) {
            parameters.remove(name);
        }
    }

    @Override
    public void addParameters(Map<String, String> params) {
        parameters.putAll(params);
    }

    @Override
    public void addParameterIfAbsent(String name, String value) {
        if (hasParameter(name)) {
            return;
        }
        parameters.put(name, value);
    }

    @Override
    public Boolean getBooleanParameter(String name, boolean defaultValue) {
        String value = getParameter(name);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }

        return Boolean.parseBoolean(value);
    }

    @Override
    public Boolean getMethodParameter(String methodName, String paramDesc, String name, boolean defaultValue) {
        String value = getMethodParameter(methodName, paramDesc, name);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    @Override
    public Integer getIntParameter(String name, int defaultValue) {
        Number n = getNumbers().get(name);
        if (n != null) {
            return n.intValue();
        }
        String value = parameters.get(name);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        int i = Integer.parseInt(value);
        getNumbers().put(name, i);
        return i;
    }

    @Override
    public Integer getMethodParameter(String methodName, String paramDesc, String name, int defaultValue) {
        String key = methodName + "(" + paramDesc + ")." + name;
        Number n = getNumbers().get(key);
        if (n != null) {
            return n.intValue();
        }
        String value = getMethodParameter(methodName, paramDesc, name);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        int i = Integer.parseInt(value);
        getNumbers().put(key, i);
        return i;
    }

    @Override
    public String getUri() {
        return protocol + Constants.PROTOCOL_SEPARATOR + host + ":" + port +
                Constants.PATH_SEPARATOR + path;
    }

    /**
     * Return service identity, if two urls have the same identity, then same service
     *
     * @return the identity
     */
    @Override
    public String getIdentity() {
        return protocol + Constants.PROTOCOL_SEPARATOR + host + ":" + port +
                "/" + getParameter(URLParamType.group.getName(), URLParamType.group.getValue()) + "/" +
                getPath() + "/" + getParameter(URLParamType.version.getName(), URLParamType.version.getValue()) +
                "/" + getParameter(URLParamType.nodeType.getName(), URLParamType.nodeType.getValue());
    }

    /**
     * check if this url can serve the refUrl.
     *
     * @param refUrl a URL object
     * @return boolean true can serve
     */
    @Override
    public boolean canServe(URL refUrl) {
        if (refUrl == null || !this.getPath().equals(refUrl.getPath())) {
            return false;
        }

        if(!protocol.equals(refUrl.getProtocol())) {
            return false;
        }

        if (!Constants.NODE_TYPE_SERVICE.equals(this.getParameter(URLParamType.nodeType.getName()))) {
            return false;
        }

        String version = getParameter(URLParamType.version.getName(), URLParamType.version.getValue());
        String refVersion = refUrl.getParameter(URLParamType.version.getName(), URLParamType.version.getValue());
        if (!version.equals(refVersion)) {
            return false;
        }

        // check serialize
        String serialize = getParameter(URLParamType.serialize.getName(), URLParamType.serialize.getValue());
        String refSerialize = refUrl.getParameter(URLParamType.serialize.getName(), URLParamType.serialize.getValue());
        if (!serialize.equals(refSerialize)) {
            return false;
        }
        // Not going to check group as cross group call is needed
        return true;
    }

    // TODO there is a trailing &, use string join instead StringBuilder.
    @Override
    public String toFullStr() {
        StringBuilder builder = new StringBuilder();
        builder.append(getUri()).append("?");

        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();

            builder.append(name).append("=").append(value).append("&");
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        return toSimpleString();
    }

    // protocol、host、port、path、group
    @Override
    public String toSimpleString() {
        return getUri() + "?group=" + getGroup();
    }

    @Override
    public boolean hasParameter(String key) {
        String p = getParameter(key);
        return p != null && p.trim().length() > 0;
    }

    /**
     * comma separated host:port pairs, e.g. "127.0.0.1:3000"
     *
     * @return server port
     */
    @Override
    public String getServerPortStr() {
        return buildHostPortStr(host, port);

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        URLImpl url = (URLImpl) o;

        if (port != url.getPort()) return false;
        if (protocol != null ? !protocol.equals(url.getProtocol()) : url.getProtocol() != null) return false;
        if (host != null ? !host.equals(url.getHost()) : url.getHost() != null) return false;
        if (path != null ? !path.equals(url.getPath()) : url.getPath() != null) return false;
        if (parameters != null ? !parameters.equals(url.getParameters()) : url.getParameters() != null) return false;
        return parameters != null ? parameters.equals(url.getParameters()) : url.getParameters() == null;
    }

    @Override
    public int hashCode() {
        int result = protocol != null ? protocol.hashCode() : 0;
        result = 31 * result + (host != null ? host.hashCode() : 0);
        result = 31 * result + port;
        result = 31 * result + (path != null ? path.hashCode() : 0);
        result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
        result = 31 * result + (numbers != null ? numbers.hashCode() : 0);
        return result;
    }

    private Map<String, Number> getNumbers() {
        if (numbers == null) {
            // create a new map.
            numbers = new ConcurrentHashMap<>();
        }
        return numbers;
    }

}
