package com.networknt.ldap;

import com.networknt.client.Http2Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class LdapSSLSocketFactory extends SSLSocketFactory {
    private static final Logger logger = LoggerFactory.getLogger(LdapSSLSocketFactory.class);
    private SSLSocketFactory socketFactory;

    public LdapSSLSocketFactory() {
        try {
            SSLContext ctx = Http2Client.createSSLContext();
            socketFactory = ctx.getSocketFactory();
        } catch ( Exception ex ){ throw new IllegalArgumentException(ex); }
    }

    public static SocketFactory getDefault() { return new LdapSSLSocketFactory(); }

    @Override public String[] getDefaultCipherSuites() { return socketFactory.getDefaultCipherSuites(); }
    @Override public String[] getSupportedCipherSuites() { return socketFactory.getSupportedCipherSuites(); }

    @Override public Socket createSocket(Socket socket, String string, int i, boolean bln) throws IOException {
        return socketFactory.createSocket(socket, string, i, bln);
    }
    @Override public Socket createSocket(String string, int i) throws IOException, UnknownHostException {
        return socketFactory.createSocket(string, i);
    }
    @Override public Socket createSocket(String string, int i, InetAddress ia, int i1) throws IOException, UnknownHostException {
        return socketFactory.createSocket(string, i, ia, i1);
    }
    @Override public Socket createSocket(InetAddress ia, int i) throws IOException {
        return socketFactory.createSocket(ia, i);
    }
    @Override public Socket createSocket(InetAddress ia, int i, InetAddress ia1, int i1) throws IOException {
        return socketFactory.createSocket(ia, i, ia1, i1);
    }
}
