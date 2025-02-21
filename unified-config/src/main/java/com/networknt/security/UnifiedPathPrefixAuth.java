package com.networknt.security;

import com.networknt.config.schema.ArrayField;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.StringField;

import java.util.List;

public class UnifiedPathPrefixAuth {

    @StringField(configFieldName = "prefix")
    String prefix;

    @BooleanField(configFieldName = "basic")
    boolean basic;

    @BooleanField(configFieldName = "jwt")
    boolean jwt;

    @BooleanField(configFieldName = "sjwt")
    boolean sjwt;

    @BooleanField(configFieldName = "swt")
    boolean swt;

    @BooleanField(configFieldName = "apikey")
    boolean apikey;

    @ArrayField(configFieldName = "jwkServiceIds", items = String.class)
    List<String> jwkServiceIds;

    @ArrayField(configFieldName = "sjwkServiceIds", items = String.class)
    List<String> sjwkServiceIds;

    @ArrayField(configFieldName = "swtServiceIds", items = String.class)
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
