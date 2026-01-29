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

    /**
     * Constructor
     */
    public QueryHeaderRewriteRule() {
    }

    /**
     * Get the old key
     * @return old key
     */
    public String getOldK() {
        return oldK;
    }

    /**
     * Set the old key
     * @param oldK old key
     */
    public void setOldK(String oldK) {
        this.oldK = oldK;
    }

    /**
     * Get the new key
     * @return new key
     */
    public String getNewK() {
        return newK;
    }

    /**
     * Set the new key
     * @param newK new key
     */
    public void setNewK(String newK) {
        this.newK = newK;
    }

    /**
     * Get the old value
     * @return old value
     */
    public String getOldV() {
        return oldV;
    }

    /**
     * Set the old value
     * @param oldV old value
     */
    public void setOldV(String oldV) {
        this.oldV = oldV;
    }

    /**
     * Get the new value
     * @return new value
     */
    public String getNewV() {
        return newV;
    }

    /**
     * Set the new value
     * @param newV new value
     */
    public void setNewV(String newV) {
        this.newV = newV;
    }
}
