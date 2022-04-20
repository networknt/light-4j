package com.networknt.handler.config;

/**
 * This is the object for the query parameter or header overwrite rules. Both are using the similar
 * type of configuration with the key is the request path.
 * @author Steve Hu
 */
public class QueryHeaderRewriteRule {
    String oldK;
    String newK;
    String oldV;
    String newV;

    public QueryHeaderRewriteRule() {
    }

    public String getOldK() {
        return oldK;
    }

    public void setOldK(String oldK) {
        this.oldK = oldK;
    }

    public String getNewK() {
        return newK;
    }

    public void setNewK(String newK) {
        this.newK = newK;
    }

    public String getOldV() {
        return oldV;
    }

    public void setOldV(String oldV) {
        this.oldV = oldV;
    }

    public String getNewV() {
        return newV;
    }

    public void setNewV(String newV) {
        this.newV = newV;
    }
}
