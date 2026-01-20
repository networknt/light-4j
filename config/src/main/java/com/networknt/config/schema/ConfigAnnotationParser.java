package com.networknt.config.schema;

import com.networknt.config.schema.generator.Generator;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Annotation processor for config modules in Light4J.
 * This processor is responsible for generating schema/configuration files based on properties defined in the POJO annotations.
 *
 * @author Kalev Gonvick
 */
@SupportedAnnotationTypes("com.networknt.config.schema.ConfigSchema")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ConfigAnnotationParser extends AbstractProcessor {

    private static final String BUILD_COMMAND_PROPERTY = "sun.java.command";
    private static final String PROFILE_FLAG = "-P";
    private static final String SCHEMA_GENERATION_PROFILE = "schema-generation";
    private boolean generated = false;

    @Override
    public synchronized void init(final ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.processingEnv = processingEnv;
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        final var configs = roundEnv.getElementsAnnotatedWith(ConfigSchema.class);
        if (roundEnv.processingOver() || this.generated) {
            return false;
        }

        var schemaEnabled = this.isSchemaProfileEnabled();
        if (!schemaEnabled) {
            return false;
        }

        for (final var config : configs) {
            final var configClassMetadata = config.getAnnotation(ConfigSchema.class);

            /* Get config path inside project folder */
            final var optionalModulePath = this.getPathInCurrentModule(
                    "../../src/main/resources/config/",
                    configClassMetadata.configKey() + "_module"
            );

            /* Get config path inside target folder */
            final var optionalTargetPathMirror = this.getPathInCurrentModule(
                    "config/",
                    configClassMetadata.configKey() + "_target"
            );

            final Path modulePath;
            final Path targetPathMirror;
            if (optionalTargetPathMirror.isPresent() && optionalModulePath.isPresent()) {
                modulePath = optionalModulePath.get();
                targetPathMirror = optionalTargetPathMirror.get();
            } else return false;

            final var configMetadata = MetadataParser.gatherObjectSchemaData(config, processingEnv)
                    .description(configClassMetadata.configDescription())
                    .build();

            for (final var output : configClassMetadata.outputFormats()) {
                final var extension = output.getExtension();

                /* write config in project folder. */
                final var optionalProjectFile = this.resolveOrCreateFile(
                        modulePath,
                        configClassMetadata.configName() + extension
                );
                final Path projectFile;
                if (optionalProjectFile.isPresent()) {
                    projectFile = optionalProjectFile.get().toPath();
                } else return false;

                try (var writer = Files.newBufferedWriter(projectFile)) {
                    Generator.getGenerator(output, configClassMetadata.configKey(), configClassMetadata.configName())
                            .writeSchemaToFile(writer, configMetadata);
                } catch (IOException e) {
                    this.logError("Error generating config file: %s", e.getMessage());
                    return false;
                }

                /* write config in target folder. */
                final var optionalTargetFile = this.resolveOrCreateFile(
                        targetPathMirror,
                        configClassMetadata.configName() + extension
                );

                final Path targetFile;
                if (optionalTargetFile.isPresent()) {
                    targetFile = optionalTargetFile.get().toPath();
                } else return false;

                try (var writer = Files.newBufferedWriter(targetFile)) {
                    Generator.getGenerator(output, configClassMetadata.configKey(), configClassMetadata.configName())
                            .writeSchemaToFile(writer, configMetadata);
                } catch (IOException e) {
                    this.logError("Error generating config file: %s", e.getMessage());
                    return false;
                }
            }
        }
        this.generated = true;
        return true;
    }

    /**
     * Resolves a file in the given path, if the file does not exist, it will be created.
     *
     * @param path     The path to the file.
     * @param fileName The name of the file.
     * @return The resolved file.
     */
    private Optional<File> resolveOrCreateFile(final Path path, final String fileName) {
        final var file = path.resolve(fileName).toFile();
        if (!file.exists()) {
            try {
                if (file.createNewFile())
                    this.logInfo("File %s created", file.getName());
                else this.logInfo("File %s already exists, replacing file content...");
            } catch (IOException e) {
                this.logError("Could not create a new file '%s' on path '%s': %s", fileName, file.toString(), e.getMessage());
                return Optional.empty();
            }
        }
        return Optional.of(file);
    }


    private Optional<Path> getPathInCurrentModule(final String relativeModulePath, final String tempAnchorName) {
        final var path = Objects.requireNonNullElse(relativeModulePath, "");
        final FileObject resource;
        try {
            resource = this.processingEnv.getFiler().createResource(
                    StandardLocation.CLASS_OUTPUT,
                    "",
                    "anchor" + tempAnchorName,
                    (Element[]) null
            );
        } catch (IOException e) {
            this.logError("Could not create the temporary anchor '%s' on path '%s': %s", tempAnchorName, path, e.getMessage());
            return Optional.empty();
        }

        // return parent folder of the temp file.
        return Optional.of(Paths.get(resource.toUri()).getParent().resolve(path));
    }

    public void logError(String message, Object... args) {
        final var formattedMessage = formatMessage(message, args);
        this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, formattedMessage);
    }

    public void logInfo(String message, Object... args) {
        final var formattedMessage = formatMessage(message, args);
        this.processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, formattedMessage);
    }

    private static String formatMessage(String message, Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }
        return String.format(message, args);
    }

    private boolean isSchemaProfileEnabled() {
        return Arrays.stream(System.getProperty(BUILD_COMMAND_PROPERTY)
                .split(" "))
                .dropWhile(arg -> !arg.equals(PROFILE_FLAG))
                .skip(1)
                .findFirst()
                .map(profileListString -> Arrays.asList(profileListString.split(",")).contains(SCHEMA_GENERATION_PROFILE))
                .orElse(false);
    }

}
