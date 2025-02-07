package com.networknt.config.schema;

import com.networknt.config.schema.generator.Generator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.*;

@SupportedAnnotationTypes("com.networknt.config.schema.ConfigSchema")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ConfigAnnotationParser extends AbstractProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigAnnotationParser.class);

    private MetadataParser metadataParser;

    private ProcessingEnvironment processingEnv;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.processingEnv = processingEnv;
        this.metadataParser = new MetadataParser();

        LOG.trace("ConfigAnnotationParser initialized");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        LOG.trace("Processing annotations");

        final var configs = roundEnv.getElementsAnnotatedWith(ConfigSchema.class);

        for (final var config : configs) {
            final var configClassMetadata = config.getAnnotation(ConfigSchema.class);
            final var configName = configClassMetadata.configKey();
            final var outputs = configClassMetadata.outputFormats();
            final var outputDir = configClassMetadata.outputDir();
            final var configMetadata = this.metadataParser.parseMetadata(config, this.processingEnv);

            for (final var output : outputs) {

                LOG.debug("Generating {} file for: {}", output.toString(), config.getSimpleName());

                Generator.getGenerator(output, configName).writeSchemaToFile(outputDir, configMetadata);
            }

        }
        return true;
    }

}
