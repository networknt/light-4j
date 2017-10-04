package com.networknt.utility;

import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static java.io.File.separator;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * NIO utility
 *
 * @author Steve Hu
 */
public class NioUtils {
    private static final int BUFFER_SIZE = 1024 * 4;

    static final Logger logger = LoggerFactory.getLogger(NioUtils.class);

    /**
     * Returns a zip file system
     * @param zipFilename to construct the file system from
     * @param create true if the zip file should be created
     * @return a zip file system
     * @throws IOException
     */
    private static FileSystem createZipFileSystem(String zipFilename, boolean create) throws IOException {

        // convert the filename to a URI
        final Path path = Paths.get(zipFilename);
        if(Files.notExists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }
        final URI uri = URI.create("jar:file:" + path.toUri().getPath());

        final Map<String, String> env = new HashMap<>();
        if (create) {
            env.put("create", "true");
        }
        return FileSystems.newFileSystem(uri, env);
    }

    /**
     * Unzips the specified zip file to the specified destination directory.
     * Replaces any files in the destination, if they already exist.
     * @param zipFilename the name of the zip file to extract
     * @param destDirname the directory to unzip to
     * @throws IOException
     */
    public static void unzip(String zipFilename, String destDirname)
            throws IOException{

        final Path destDir = Paths.get(destDirname);
        //if the destination doesn't exist, create it
        if(Files.notExists(destDir)){
            if(logger.isDebugEnabled()) logger.debug(destDir + " does not exist. Creating...");
            Files.createDirectories(destDir);
        }

        try (FileSystem zipFileSystem = createZipFileSystem(zipFilename, false)){
            final Path root = zipFileSystem.getPath("/");

            //walk the zip file tree and copy files to the destination
            Files.walkFileTree(root, new SimpleFileVisitor<Path>(){
                @Override
                public FileVisitResult visitFile(Path file,
                                                 BasicFileAttributes attrs) throws IOException {
                    final Path destFile = Paths.get(destDir.toString(),
                            file.toString());
                    if(logger.isDebugEnabled()) logger.debug("Extracting file %s to %s", file, destFile);
                    Files.copy(file, destFile, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir,
                                                         BasicFileAttributes attrs) throws IOException {
                    final Path dirToCreate = Paths.get(destDir.toString(),
                            dir.toString());
                    if(Files.notExists(dirToCreate)){
                        if(logger.isDebugEnabled()) logger.debug("Creating directory %s", dirToCreate);
                        Files.createDirectory(dirToCreate);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }


    /**
     * Creates/updates a zip file.
     * @param zipFilename the name of the zip to create
     * @param filenames list of filename to add to the zip
     * @throws IOException
     */
    public static void create(String zipFilename, String... filenames)
            throws IOException {

        try (FileSystem zipFileSystem = createZipFileSystem(zipFilename, true)) {
            final Path root = zipFileSystem.getPath("/");

            //iterate over the files we need to add
            for (String filename : filenames) {
                final Path src = Paths.get(filename);

                //add a file to the zip file system
                if(!Files.isDirectory(src)){
                    final Path dest = zipFileSystem.getPath(root.toString(),
                            src.toString());
                    final Path parent = dest.getParent();
                    if(Files.notExists(parent)){
                        if(logger.isDebugEnabled()) logger.debug("Creating directory %s", parent);
                        Files.createDirectories(parent);
                    }
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                }
                else{
                    //for directories, walk the file tree
                    Files.walkFileTree(src, new SimpleFileVisitor<Path>(){
                        @Override
                        public FileVisitResult visitFile(Path file,
                                                         BasicFileAttributes attrs) throws IOException {
                            final Path dest = zipFileSystem.getPath(root.toString(),
                                    file.toString());
                            Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult preVisitDirectory(Path dir,
                                                                 BasicFileAttributes attrs) throws IOException {
                            final Path dirToCreate = zipFileSystem.getPath(root.toString(),
                                    dir.toString());
                            if(Files.notExists(dirToCreate)){
                                if(logger.isDebugEnabled()) logger.debug("Creating directory %s\n", dirToCreate);
                                Files.createDirectories(dirToCreate);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
                }
            }
        }
    }

    /**
     * List the contents of the specified zip file
     * @param zipFilename
     * @throws IOException
     */
    public static void list(String zipFilename) throws IOException{

        if(logger.isDebugEnabled()) logger.debug("Listing Archive:  %s",zipFilename);
        //create the file system
        try (FileSystem zipFileSystem = createZipFileSystem(zipFilename, false)) {

            final Path root = zipFileSystem.getPath("/");

            //walk the file tree and print out the directory and filenames
            Files.walkFileTree(root, new SimpleFileVisitor<Path>(){
                @Override
                public FileVisitResult visitFile(Path file,
                                                 BasicFileAttributes attrs) throws IOException {
                    print(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir,
                                                         BasicFileAttributes attrs) throws IOException {
                    print(dir);
                    return FileVisitResult.CONTINUE;
                }

                /**
                 * prints out details about the specified path
                 * such as size and modification time
                 * @param file
                 * @throws IOException
                 */
                private void print(Path file) throws IOException{
                    final DateFormat df = new SimpleDateFormat("MM/dd/yyyy-HH:mm:ss");
                    final String modTime= df.format(new Date(
                            Files.getLastModifiedTime(file).toMillis()));
                    if(logger.isDebugEnabled()) {
                        logger.debug("%d  %s  %s",
                                Files.size(file),
                                modTime,
                                file);
                    }
                }
            });
        }
    }


    public static void deleteOldFiles(String dirPath, int olderThanMinute)  {

        File folder = new File(dirPath);
        if (folder.exists()) {
            File[] listFiles = folder.listFiles();
            long eligibleForDeletion = System.currentTimeMillis() - (olderThanMinute * 60 * 1000L);
            for (File listFile: listFiles) {
                if (listFile.lastModified() < eligibleForDeletion) {
                    if (!listFile.delete()) {
                        logger.error("Unable to delete file %s", listFile);
                    }
                }
            }
        }
    }

    public static ByteBuffer toByteBuffer(String s) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(s.length());
        buffer.put(s.getBytes(UTF_8));
        buffer.flip();
        return buffer;
    }

    public static String getTempDir() {
        // default is user home directory
        String tempDir = System.getProperty("user.home");
        try{
            //create a temp file
            File temp = File.createTempFile("A0393939", ".tmp");
            //Get tempropary file path
            String absolutePath = temp.getAbsolutePath();
            tempDir = absolutePath.substring(0,absolutePath.lastIndexOf(File.separator));
        }catch(IOException e){}
        return tempDir;
    }

    /**
     * Reads and returns the rest of the given input stream as a byte array.
     * Caller is responsible for closing the given input stream.
     */
    public static byte[] toByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            byte[] b = new byte[BUFFER_SIZE];
            int n = 0;
            while ((n = is.read(b)) != -1) {
                output.write(b, 0, n);
            }
            return output.toByteArray();
        } finally {
            output.close();
        }
    }

    /**
     * Reads and returns the rest of the given input stream as a string.
     * Caller is responsible for closing the given input stream.
     */
    public static String toString(InputStream is) throws IOException {
        return new String(toByteArray(is), StandardCharsets.UTF_8);
    }

}
