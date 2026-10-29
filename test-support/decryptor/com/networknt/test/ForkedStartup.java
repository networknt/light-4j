package com.networknt.test;

import java.io.File;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Runs public startup APIs in a fresh JVM, including with Maven's non-forking test runner. */
public final class ForkedStartup {
    private ForkedStartup() { }

    public record Result(int exitCode, String output) { }

    public static Result run(Class<?> main, Path directory, String... arguments) throws Exception {
        var paths = new LinkedHashSet<String>();
        paths.addAll(Arrays.asList(System.getProperty("java.class.path").split(File.pathSeparator)));
        for (ClassLoader loader = main.getClassLoader(); loader != null; loader = loader.getParent()) {
            if (loader instanceof URLClassLoader urls) {
                for (var url : urls.getURLs()) {
                    if (url.getProtocol().equals("file")) paths.add(Path.of(url.toURI()).toString());
                }
            }
        }
        var command = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", String.join(File.pathSeparator, paths),
                "-Dlight-4j-config-dir=" + directory, main.getName()));
        command.addAll(List.of(arguments));
        Path output = Files.createTempFile(directory, "startup-", ".log");
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            if (!process.waitFor(30, TimeUnit.SECONDS)) throw new AssertionError("Startup probe timed out");
            return new Result(process.exitValue(), Files.readString(output));
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }
}
