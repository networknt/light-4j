package com.networknt.security;

import java.util.List;

public class UnifiedPathPrefixAuth {
    String prefix;
    boolean basic;
    boolean jwt;
    boolean sjwt;
    boolean swt;
    boolean apikey;
    List<String> jwkServiceIds;
    List<String> sjwkServiceIds;
    List<String> swtServiceIds;

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public boolean isBasic() {
        return basic;
    }

    public void setBasic(boolean basic) {
        this.basic = basic;
    }

    public boolean isJwt() {
        return jwt;
    }

    public void setJwt(boolean jwt) {
        this.jwt = jwt;
    }

    public boolean isSjwt() {
        return sjwt;
    }

    public void setSjwt(boolean sjwt) {
        this.sjwt = sjwt;
    }

    public boolean isSwt() {
        return swt;
    }

    public void setSwt(boolean swt) {
        this.swt = swt;
    }

    public boolean isApikey() {
        return apikey;
    }

    public void setApikey(boolean apikey) {
        this.apikey = apikey;
    }

    public List<String> getJwkServiceIds() {
        return jwkServiceIds;
    }

    public void setJwkServiceIds(List<String> jwkServiceIds) {
        this.jwkServiceIds = jwkServiceIds;
    }

    public List<String> getSwtServiceIds() {
        return swtServiceIds;
    }

    public void setSwtServiceIds(List<String> swtServiceIds) {
        this.swtServiceIds = swtServiceIds;
    }

    public List<String> getSjwkServiceIds() {
        return sjwkServiceIds;
    }

    public void setSjwkServiceIds(List<String> sjwkServiceIds) {
        this.sjwkServiceIds = sjwkServiceIds;
    }
}
