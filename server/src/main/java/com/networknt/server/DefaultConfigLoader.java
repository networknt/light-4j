package com.networknt.server;

import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.utility.Util;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.nio.charset.StandardCharsets.UTF_8;


/**
 * Default Config Loader to fetch and load configs from config server
 *
 * To use this Config Loader, please add it to startup.yml as configLoaderClass: com.networknt.server.DefaultConfigLoader
 * so that Server class can find, instantiate and the trigger its init() method.
 */
public class DefaultConfigLoader implements IConfigLoader{
    static final Logger logger = LoggerFactory.getLogger(Server.class);

    static final String LIGHT_CONFIG_SERVER_URI = "light-config-server-uri";
    public static final String DEFAULT_ENV = "test";
    public static String lightEnv = System.getProperty("light-env");

    /**
     * Load config files from light-config-server instance. This is normally only
     * used when you run light-4j server as standalone java process. If the server
     * is dockerized and orchestrated by Kubernetes, the config files and secret
     * will be mapped to Kubernetes ConfigMap and Secret and passed into the
     * container.
     * <p>
     * Of course, you can still use it with standalone docker container but it is
     * not recommended.
     */
    @Override
    public void init() {
        // if it is necessary to load config files from config server
        // Here we expect at least env(dev/sit/uat/prod) and optional config server url
        if (lightEnv == null) {
            logger.warn("Warning! No light-env has been passed in from command line. Defaulting to {}",DEFAULT_ENV);
            lightEnv = DEFAULT_ENV;
        }

        String configUri = System.getProperty(LIGHT_CONFIG_SERVER_URI);
        if (configUri != null) {
            // try to get config files from the server.
            String targetMergeDirectory = System.getProperty(Config.LIGHT_4J_CONFIG_DIR);
            if (targetMergeDirectory == null) {
                logger.warn("Warning! No light-4j-config-dir has been passed in from command line.");
                return;
            }
            String version = Util.getJarVersion();
            String service = Server.config.getServiceId();
            String tempDir = System.getProperty("java.io.tmpdir");
            String zipFile = tempDir + "/config.zip";
            // /v1/config/1.2.4/dev/com.networknt.petstore-1.0.0

            String path = "/v1/config/" + version + "/" + lightEnv + "/" + service;
            Http2Client client = Http2Client.getInstance();
            ClientConnection connection = null;
            try {
                connection = client.connect(new URI(configUri), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL,
                        OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
            } catch (Exception e) {
                logger.error("Exeption:", e);
            }
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<ClientResponse> reference = new AtomicReference<>();
            try {
                ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(path);
                request.getRequestHeaders().put(Headers.HOST, "localhost");
                connection.sendRequest(request, client.createClientCallback(reference, latch));
                latch.await();
                int statusCode = reference.get().getResponseCode();

                if (statusCode >= 300) {
                    logger.error("Failed to load config from config server" + statusCode + ":"
                            + reference.get().getAttachment(Http2Client.RESPONSE_BODY));
                    throw new Exception("Failed to load config from config server: " + statusCode);
                } else {
                    // TODO test it out
                    FileOutputStream fos = new FileOutputStream(zipFile);
                    fos.write(reference.get().getAttachment(Http2Client.RESPONSE_BODY).getBytes(UTF_8));
                    fos.close();
                    unzipFile(zipFile, targetMergeDirectory);
                }
            } catch (Exception e) {
                logger.error("Exception:", e);
            } finally {
                IoUtils.safeClose(connection);
            }
        } else {
            logger.info("light-config-server-uri is missing in the command line. Use local config files");
        }
    }

    private static void unzipFile(String path, String target) {
        // Open the file
        try (ZipFile file = new ZipFile(path)) {
            FileSystem fileSystem = FileSystems.getDefault();
            // Get file entries
            Enumeration<? extends ZipEntry> entries = file.entries();

            // We will unzip files in this folder
            Files.createDirectory(fileSystem.getPath(target));

            // Iterate over entries
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                // If directory then create a new directory in uncompressed folder
                if (entry.isDirectory()) {
                    System.out.println("Creating Directory:" + target + entry.getName());
                    Files.createDirectories(fileSystem.getPath(target + entry.getName()));
                }
                // Else create the file
                else {
                    InputStream is = file.getInputStream(entry);
                    BufferedInputStream bis = new BufferedInputStream(is);
                    String uncompressedFileName = target + entry.getName();
                    Path uncompressedFilePath = fileSystem.getPath(uncompressedFileName);
                    Files.createFile(uncompressedFilePath);
                    FileOutputStream fileOutput = new FileOutputStream(uncompressedFileName);
                    while (bis.available() > 0) {
                        fileOutput.write(bis.read());
                    }
                    fileOutput.close();
                    System.out.println("Written :" + entry.getName());
                }
            }
        } catch (IOException e) {
            logger.error("IOException", e);
        }
    }
}
