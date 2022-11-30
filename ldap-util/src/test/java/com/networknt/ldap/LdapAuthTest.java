package com.networknt.ldap;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.*;
import java.util.Hashtable;

public class LdapAuthTest {
    static final Logger logger = LoggerFactory.getLogger(LdapAuthTest.class);

    @BeforeClass
    public static void setup() throws Exception {
        ApacheDirectoryServer.startServer();
    }

    private final static String ldapURI = "ldaps://localhost:10636/ou=users,dc=undertow,dc=io";
    private final static String contextFactory = "com.sun.jndi.ldap.LdapCtxFactory";

    private static DirContext ldapContext () throws Exception {
        Hashtable<String,String> env = new Hashtable <String,String>();
        return ldapContext(env);
    }

    private static DirContext ldapContext (Hashtable <String,String>env) throws Exception {
        env.put(Context.INITIAL_CONTEXT_FACTORY, contextFactory);
        env.put(Context.PROVIDER_URL, ldapURI);
        if(ldapURI.toUpperCase().startsWith("LDAPS://")) {
            env.put(Context.SECURITY_PROTOCOL, "ssl");
            env.put("java.naming.ldap.factory.socket", "com.networknt.ldap.LdapSSLSocketFactory");
        }
        env.put(Context.SECURITY_PRINCIPAL, "uid=oauth,ou=users,dc=undertow,dc=io");
        env.put(Context.SECURITY_CREDENTIALS, "theoauth");

        DirContext ctx = new InitialDirContext(env);
        return ctx;
    }

    private static String getUid (String user) throws Exception {
        DirContext ctx = ldapContext();

        String filter = "(uid=" + user + ")";
        SearchControls ctrl = new SearchControls();
        ctrl.setSearchScope(SearchControls.SUBTREE_SCOPE);
        NamingEnumeration answer = ctx.search("", filter, ctrl);

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
        env.put(Context.PROVIDER_URL, ldapURI);
        if(ldapURI.toUpperCase().startsWith("LDAPS://")) {
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
        catch (AuthenticationException e) {
            return false;
        } finally {
            try {
                if(ctx != null) ctx.close();
            } catch(Exception e) {}
        }
        return true;
    }

    @Ignore
    @Test
    public void testAuthentication() throws Exception {
        String user = "jduke";
        String password = "theduke";
        String dn = getUid( user );

        if (dn != null) {
            /* Found user - test password */
            if ( testBind( dn, password ) ) {
                System.out.println( "user '" + user + "' authentication succeeded" );
            }
            else {
                System.out.println( "user '" + user + "' authentication failed" );
                System.exit(1);
            }
        }
        else {
            System.out.println( "user '" + user + "' not found" );
            System.exit(1);
        }
    }

    @Test
    @Ignore
    public void testAuthorization() throws Exception {
        String uid = "jduke";
        String domainName = "undertow.io";
        DirContext ctx = ldapContext();
        try {
            SearchControls ctrls = new SearchControls();
            ctrls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            NamingEnumeration<SearchResult> results = ctx.search("","(& (uid="+uid+")(objectClass=person))", ctrls);
            if(!results.hasMore())
                throw new AuthenticationException("Principal name not found");

            SearchResult result = results.next();
            System.out.println("distinguisedName: " + result.getNameInNamespace() ); // CN=Firstname Lastname,OU=Mycity,DC=mydomain,DC=com

            Attribute memberOf = result.getAttributes().get("memberOf");
            if(memberOf!=null) {
                for(int idx=0; idx<memberOf.size(); idx++) {
                    System.out.println("memberOf: " + memberOf.get(idx).toString() ); // CN=Mygroup,CN=Users,DC=mydomain,DC=com
                    //Attribute att = context.getAttributes(memberOf.get(idx).toString(), new String[]{"CN"}).get("CN");
                    //System.out.println( att.get().toString() ); //  CN part of groupname
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if(ctx != null) ctx.close();
            } catch(Exception e) {}
        }
    }

    /**
     * Create "DC=sub,DC=mydomain,DC=com" string
     * @param domainName    sub.mydomain.com
     * @return
     */
    private static String toDC(String domainName) {
        StringBuilder buf = new StringBuilder();
        for (String token : domainName.split("\\.")) {
            if(token.length()==0) continue;
            if(buf.length()>0)  buf.append(",");
            buf.append("DC=").append(token);
        }
        return buf.toString();
    }
}
