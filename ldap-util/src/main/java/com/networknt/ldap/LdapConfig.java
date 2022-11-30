package com.networknt.ldap;

public class LdapConfig {
    String uri;
    String domain;
    String principal;
    String credential;
    String searchFilter;
    String searchBase;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getDomain() { return domain; }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }

    public String getSearchFilter() { return searchFilter; }

    public void setSearchFilter(String searchFilter) { this.searchFilter = searchFilter; }

    public String getSearchBase() { return searchBase; }

    public void setSearchBase(String searchBase) { this.searchBase = searchBase; }
}
