package com.networknt.ldap;

import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.*;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

/**
 * A utility class that interacts with LDAP server for authentication and authorization.
 *
 * @author Steve Hu
 */
public class LdapUtil {
    private final static Logger logger = LoggerFactory.getLogger(LdapUtil.class);
    private final static String contextFactory = "com.sun.jndi.ldap.LdapCtxFactory";
    private final static String CONFIG_LDAP = "ldap";

    private final static LdapConfig config = (LdapConfig)Config.getInstance().getJsonObjectConfig(CONFIG_LDAP, LdapConfig.class);
    /**
     * Bind the username and password with LDAP context to verify the password. Return true
     * if there is no NamingException. No further activity to retrieve group or memberOf
     * from LDAP server.
     *
     * @param username String
     * @param password String
     * @return boolean true if authenticated
     */
    public static boolean authenticate(String username, String password) {
        try {
            String dn = getUid(username);
            if (dn != null) {
                /* Found user - test password */
                if ( testBind( dn, password ) ) {
                    if(logger.isDebugEnabled()) logger.debug("user '" + username + "' authentication succeeded");
                    return true;
                } else {
                    if(logger.isDebugEnabled()) logger.debug("user '" + username + "' authentication failed");
                    return false;
                }
            } else {
                if(logger.isDebugEnabled()) logger.debug("user '" + username + "' not found");
                return false;
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
            return false;
        }
    }

    /**
     *
     * @param username String
     * @return A set of memberOf attributes for the username on LDAP server. You can only call
     * this method if the username has been authenticated with SPNEGO/Kerberos
     */
    public static Set<String> authorize(String username) {
        Set<String> groups = new HashSet();
        DirContext ctx = null;
        try {
            ctx = ldapContext();
            SearchControls ctrls = new SearchControls();
            ctrls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            String filter = String.format(config.searchFilter, username);
            NamingEnumeration<SearchResult> results = ctx.search(config.searchBase, filter, ctrls);
            if(!results.hasMore()) {
                logger.error("Principal name '" + username + "' not found");
                return null;
            }
            SearchResult result = results.next();
            if(logger.isDebugEnabled()) logger.debug("distinguisedName: " + result.getNameInNamespace());

            Attribute memberOf = result.getAttributes().get("memberOf");
            if(memberOf!=null) {
                for(int idx=0; idx<memberOf.size(); idx++) {
                    groups.add(memberOf.get(idx).toString());
                }
            }
        } catch (Exception e) {
            logger.error("Failed to authorize user " + username, e);
            return null;
        } finally {
            try {
                if(ctx != null) ctx.close();
            } catch(Exception e) {}
        }
        return groups;
    }

    /**
     * First authenticate with the username and password on LDAP server and then retrieve groups
     * or memberOf attributes from LDAP server. return null if authentication is failed. return
     * an empty set if there is no group available for the current user. This method combines both
     * authentication and authorization together.
     *
     * @param username String
     * @param password String
     * @return A set of memberOf attributes for the username after authentication.
     */
    public static Set<String> auth(String username, String password) {

        return null;
    }

    private static DirContext ldapContext () throws Exception {
        Hashtable<String,String> env = new Hashtable <String,String>();
        return ldapContext(env);
    }

    private static DirContext ldapContext (Hashtable<String,String> env) throws Exception {
        env.put(Context.INITIAL_CONTEXT_FACTORY, contextFactory);
        env.put(Context.PROVIDER_URL, config.getUri());
        if(config.getUri().toUpperCase().startsWith("LDAPS://")) {
            env.put(Context.SECURITY_PROTOCOL, "ssl");
            env.put("java.naming.ldap.factory.socket", "com.networknt.ldap.LdapSSLSocketFactory");
        }
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, config.getPrincipal());
        env.put(Context.SECURITY_CREDENTIALS, config.getCredential());

        DirContext ctx = new InitialDirContext(env);
        return ctx;
    }

    private static String getUid (String username) throws Exception {
        DirContext ctx = ldapContext();
        String filter = String.format(config.searchFilter, username);
        SearchControls ctrl = new SearchControls();
        ctrl.setSearchScope(SearchControls.SUBTREE_SCOPE);
        NamingEnumeration answer = ctx.search(config.searchBase, filter, ctrl);

        String dn;
        if (answer.hasMore()) {
            SearchResult result = (SearchResult) answer.next();
            dn = result.getNameInNamespace();
        }
        else {
            dn = null;
        }
        answer.close();
        return dn;
    }

    private static boolean testBind (String dn, String password) throws Exception {
        Hashtable<String,String> env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, contextFactory);
        env.put(Context.PROVIDER_URL, config.getUri());
        if(config.getUri().toUpperCase().startsWith("LDAPS://")) {
            env.put(Context.SECURITY_PROTOCOL, "ssl");
            env.put("java.naming.ldap.factory.socket", "com.networknt.ldap.LdapSSLSocketFactory");
        }
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, dn);
        env.put(Context.SECURITY_CREDENTIALS, password);
        DirContext ctx = null;
        try {
            ctx = new InitialDirContext(env);
        }
        catch (javax.naming.AuthenticationException e) {
            return false;
        } finally {
            try {
                if(ctx != null) ctx.close();
            } catch(Exception e) {}
        }
        return true;
    }


}
