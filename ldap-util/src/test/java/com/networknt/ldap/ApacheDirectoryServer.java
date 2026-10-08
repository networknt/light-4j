package com.networknt.ldap;

import com.networknt.config.Config;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.factory.DirectoryServiceFactory;
import org.apache.directory.server.core.factory.PartitionFactory;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Utility class to start up an LDAPS directory server for LDAP tests.
 *
 * It is better to start the server once instead of once per test but once running
 * the overhead is minimal. However a better solution may be to use the suite runner.
 *
 * TODO - May be able to add some lifecycle methods to DefaultServer to allow
 * for an extension.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ApacheDirectoryServer {

    static final int LDAP_PORT = 11389;
    static final int LDAPS_PORT = 10636;

    private static final String DIRECTORY_NAME = "Test Service";
    private static boolean initialised;
    private static Path workingDir;

    /*
     * LDAP Related
     */
    private static DirectoryService directoryService;
    private static LdapServer ldapServer;

    public static boolean startServer() throws Exception {
        if (initialised) {
            return false;
        }
        startLdapServer();

        initialised = true;
        return true;
    }

    private static void startLdapServer() throws Exception {
        createWorkingDir();
        DirectoryServiceFactory dsf = new DefaultDirectoryServiceFactory();
        dsf.init(DIRECTORY_NAME);
        directoryService = dsf.getDirectoryService();
        directoryService.getChangeLog().setEnabled(false);
        SchemaManager schemaManager = directoryService.getSchemaManager();

        createPartition(dsf, schemaManager, "users", "ou=users,dc=undertow,dc=io");

        CoreSession adminSession = directoryService.getAdminSession();
        //Map<String, String> mappings = Collections.singletonMap("hostname", DefaultServer.getDefaultServerAddress().getHostString());
        Map<String, String> mappings = Collections.singletonMap("hostname", "localhost");
        processLdif(schemaManager, adminSession, "partition.ldif", mappings);
        // The legacy user data retains Kerberos attributes, but this fixture tests LDAP only.
        processLdif(schemaManager, adminSession, "user.ldif", mappings);

        ldapServer = new LdapServer();
        ldapServer.setServiceName("DefaultLDAP");
        Transport ldap = new TcpTransport( "0.0.0.0", LDAPS_PORT, 3, 5 );
        ldap.enableSSL(true);
        ldapServer.addTransports(ldap);
        ldapServer.setKeystoreFile(ApacheDirectoryServer.class.getResource("/config/server.keystore").getFile());
        ldapServer.setCertificatePassword("password");
        ldapServer.setDirectoryService(directoryService);
        ldapServer.start();
    }

    private static void createPartition(final DirectoryServiceFactory dsf, final SchemaManager schemaManager, final String id,
                                        final String suffix) throws Exception {
        PartitionFactory pf = dsf.getPartitionFactory();
        Partition p = pf.createPartition(schemaManager, directoryService.getDnFactory(), id, suffix, 1000, workingDir.toFile());
        p.initialize();
        directoryService.addPartition(p);
    }

    private static void processLdif(final SchemaManager schemaManager, final CoreSession adminSession, final String ldifName,
                                    final Map<String, String> mappings) throws Exception {
        InputStream resourceInput = ApacheDirectoryServer.class.getResourceAsStream("/ldif/" + ldifName);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(resourceInput.available());
        int current;
        while ((current = resourceInput.read()) != -1) {
            if (current == '$') {
                // Enter String replacement mode.
                int second = resourceInput.read();
                if (second == '{') {
                    ByteArrayOutputStream substitute = new ByteArrayOutputStream();
                    while ((current = resourceInput.read()) != -1 && current != '}') {
                        substitute.write(current);
                    }
                    if (current == -1) {
                        baos.write(current);
                        baos.write(second);
                        baos.write(substitute.toByteArray()); // Terminator never found.
                    }
                    String toReplace = new String(substitute.toByteArray(), UTF_8);
                    if (mappings.containsKey(toReplace)) {
                        baos.write(mappings.get(toReplace).getBytes(UTF_8));
                    } else {
                        throw new IllegalArgumentException(String.format("No mapping found for '%s'", toReplace));
                    }
                } else {
                    baos.write(current);
                    baos.write(second);
                }
            } else {
                baos.write(current);
            }
        }

        ByteArrayInputStream ldifInput = new ByteArrayInputStream(baos.toByteArray());
        LdifReader ldifReader = new LdifReader(ldifInput);
        for (LdifEntry ldifEntry : ldifReader) {
            adminSession.add(new DefaultEntry(schemaManager, ldifEntry.getEntry()));
        }
        ldifReader.close();
        ldifInput.close();
    }

    private static void createWorkingDir() throws IOException {
        if (workingDir == null) {
            workingDir = Paths.get(".", "target", "apacheds_working");
            if (!Files.exists(workingDir)) {
                Files.createDirectories(workingDir);
            }
        }
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(workingDir)) {
            for(Path child : stream) {
                Files.delete(child);
            }
        }
    }
}
