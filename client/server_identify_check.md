# Server identify check in CaaS environments

To prevent man-in-the-middle attacks, many protocols (e.g., HTTPS) over TLS require server identities to be checked. This check ensures that the hostname in a server certificate matches its hostname. Java SSLEngine supports the identity check but does not enable it by default. We need to set proper SSL parameters to enable it.

That being said, simply enabling the standard identify check is not enough in some CaaS environments. The reason is that servers do not have static hostnames in those environments. In this case, X509 certificates can not be created since they require static names (in common names or subject alternative names) to identify servers. We need another way to fulfill identify check.

In light4j-client, we provide a way to check server identities based on service names or IDs. When creating server certificates, the service names or IDs are used as common names or subject alternative names. In the client, users can specify the names that it trusts. When the client receives server certificates, the user specified names are used in the server identity check. This approach decouples server identity check from server hostnames. That is, clients can use any valid hostname or IP addresses to call services.

# User guide
In light4j-client, the server identify check is controlled by three configuration items.

* boolean loadTrustStore

This specifies whether TLS is enabled or not (although client trust store is not mandatory in one-way TLS). The server identity check is only applied when TLS is enabled.

* boolean verifyHostname

This specifies whether server identify check is enabled or not.

* String trustedNames

This specifies a comma delimited list of trusted names. These names are used to do the server identity check if configured. If this is not configured (i.e., trustedNames is blank), the standartd algorithm are used. That is, server identify check is done using hostnames.

# enhancement of TLS handshake for HTTP2 in undertow

When using undertow HTTP2 with Java 8, connections are exposed to users before the handshaking is completed. This might because Application-Layer Protocol Negotiation (ALPN) protocol is not supported in Java 8. Although, Undertow provides a set of `ALPNHack*` classes to provide ALPN services for Java 8. But it looks like that is not enough.

As the handshaking is controlled privately in undertow classes, we end up rewrote the three undertow classes, ALPNClientSelector, Http2ClientProvider, and HttpClientProvider. In the re-written classes, connections are only returned to users after handshaking is completed successfully. If the handshaking fails, a `ClosedChannelException` will be thrown. These classes are meant to be used with Java 8 only and need to be reviewed if Java 9 or later is used.