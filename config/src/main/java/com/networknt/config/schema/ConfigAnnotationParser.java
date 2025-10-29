package com.networknt.config.schema;

import com.networknt.config.schema.generator.Generator;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
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

    private MetadataParser metadataParser;
    private static final String BUILD_COMMAND_PROPERTY = "sun.java.command";
    private static final String PROFILE_FLAG = "-P";
    private static final String SCHEMA_GENERATION_PROFILE = "schema-generation";

    private ProcessingEnvironment processingEnv;
    private boolean generated = false;

    @Override
    public synchronized void init(final ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.processingEnv = processingEnv;
        this.metadataParser = new MetadataParser();
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {

        final var configs = roundEnv.getElementsAnnotatedWith(ConfigSchema.class);
        if (roundEnv.processingOver() || this.generated) {
            return true;
        }

        var schemaEnabled = false;
        final var javaCmd = System.getProperty(BUILD_COMMAND_PROPERTY);
        final var cmdProps = javaCmd.split(" ");
        if (cmdProps.length == 0) {
            return true;
        }

        var x = 0;
        var profilesDefined = false;
        for (;x < cmdProps.length; x++) {
            if (cmdProps[x].equals(PROFILE_FLAG)) {
                x = x + 1;
                profilesDefined = true;
                break;
            }
        }

        if (!profilesDefined) {
            AnnotationUtils.logWarning("No profiles defined, skipping schema-generation..");
            return true;
        }

        final var profileArr = cmdProps[x].split(",");
        for (var profile : profileArr) {
            if (profile.equals(SCHEMA_GENERATION_PROFILE)) {
                schemaEnabled = true;
                break;
            }
        }

        if (!schemaEnabled) {
            AnnotationUtils.logInfo("%s profile is disabled, skipping schema generation...", SCHEMA_GENERATION_PROFILE);
            return true;
        }

        for (final var config : configs) {

            final var configClassMetadata = config.getAnnotation(ConfigSchema.class);

            /* Get config path inside project folder */
            final var modulePath = this.getPathInCurrentModule(
                    "../../src/main/resources/config/",
                    configClassMetadata.configKey() + "_module"
            );

            /* Get config path inside target folder */
            final var targetPathMirror = this.getPathInCurrentModule(
                    "config/",
                    configClassMetadata.configKey() + "_target"
            );

            /* Generate a file inside the project folder and inside the target folder. */
            final var configMetadata = this.metadataParser.parseMetadata(config, this.processingEnv);

            AnnotationUtils.updateIfNotDefault(
                    configMetadata,
                    configClassMetadata.configDescription(),
                    MetadataParser.DESCRIPTION_KEY,
                    ConfigSchema.DEFAULT_STRING
            );

            for (final var output : configClassMetadata.outputFormats()) {
                final var extension = output.getExtension();

                /* write config in project folder. */
                final var projectFile = this.resolveOrCreateFile(
                        modulePath,
                        configClassMetadata.configName() + extension
                ).toPath();

                try (var writer = Files.newBufferedWriter(projectFile)) {
                    Generator.getGenerator(output, configClassMetadata.configKey(), configClassMetadata.configName())
                            .writeSchemaToFile(writer, configMetadata);
                } catch (IOException e) {
                    throw new RuntimeException("Error generating config file", e);
                }

                /* write config in target folder. */
                final var targetFile = this.resolveOrCreateFile(
                        targetPathMirror,
                        configClassMetadata.configName() + extension
                ).toPath();

                try (var writer = Files.newBufferedWriter(targetFile)) {
                    Generator.getGenerator(output, configClassMetadata.configKey(), configClassMetadata.configName())
                            .writeSchemaToFile(writer, configMetadata);
                } catch (IOException e) {
                    throw new RuntimeException("Error generating config file", e);
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
    private File resolveOrCreateFile(final Path path, final String fileName) {
        final var file = path.resolve(fileName).toFile();

        if (!file.exists()) {
            try {

                if (file.createNewFile())
                    AnnotationUtils.logInfo("File %s created", file.getName());

                else AnnotationUtils.logInfo("File %s already exists, replacing file content...");

            } catch (IOException e) {
                throw new RuntimeException(
                        "Could not create a new file '" +
                                fileName +
                                "', for the directory '" +
                                file + "'. " +
                                e.getMessage()
                );
            }
        }

        return file;
    }


    private Path getPathInCurrentModule(final String relativeModulePath, final String tempAnchorName) {
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
            throw new RuntimeException("Could not create a temporary anchor '"+ tempAnchorName +"' under path '" + path + "'.", e);
        }

        // return parent folder of the temp file.
        return Paths.get(resource.toUri()).getParent().resolve(path);
    }

}
