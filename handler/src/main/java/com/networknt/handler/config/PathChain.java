package com.networknt.handler.config;

import com.networknt.utility.NetUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Nicholas Azar
 */
public class PathChain {

    private String source;
    private String path;
    private String method;
    private List<String> exec;

    public String getSource() { return source; }

    public void setSource(String source) { this.source = source; }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getExec() {
        return exec;
    }

    public void setExec(List<String> exec) {
        this.exec = exec;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) { this.method = method; }

    @Override
    public String toString() {
        if(path != null) {
            return path + "@" + method + " → " + exec;
        } else {
            return source + "() → " + exec;
        }
    }
    /**
     * Validate the settings and raise Exception on error.
     * The origin is used to help locate problems.
     */
    public void validate(String origin) {
        List<String> problems = new ArrayList<>();
        if(source == null) {
            if(path == null) {
                problems.add("You must specify either path or source");
            } else if(method == null) {
                problems.add("You must specify method along with path: " + path);
            }
        } else {
            if(path != null) {
                problems.add("Conflicting source: " + source + " and path: " + path);
            }
            if(method != null) {
                problems.add("Conflicting source: " + source + " and method: " + method);
            }
        }
        if(method != null && !NetUtils.METHODS.contains(method.toUpperCase())) {
            problems.add("Invalid HTTP method: " + method);
        }
        if(!problems.isEmpty()) {
            throw new RuntimeException("Bad paths element in " + origin + " [ " + String.join(" | ", problems) + " ]");
        }
    }
}
