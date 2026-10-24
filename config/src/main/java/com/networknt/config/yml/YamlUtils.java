package com.networknt.config.yml;

import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.resolver.CoreScalarResolver;
import org.snakeyaml.engine.v2.resolver.ScalarResolver;
import org.snakeyaml.engine.v2.schema.CoreSchema;
import org.snakeyaml.engine.v2.representer.StandardRepresenter;
import org.snakeyaml.engine.v2.nodes.Tag;

import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

/** Shared YAML 1.2 Core settings for configuration readers and writers. */
public final class YamlUtils {
    /** Set false to reject duplicates; true preserves classic SnakeYAML's last-value-wins policy. */
    public static final String ALLOW_DUPLICATE_KEYS = "light-4j-yaml-allow-duplicate-keys";

    private YamlUtils() {
    }

    public static LoadSettings loadSettings(boolean decrypt) {
        CoreSchema schema = decrypt ? new CoreSchema() {
            @Override
            public ScalarResolver getScalarResolver() {
                CoreScalarResolver resolver = new CoreScalarResolver(true);
                resolver.addImplicitResolver(YmlConstants.CRYPT_TAG, YmlConstants.CRYPT_PATTERN,
                        YmlConstants.CRYPT_FIRST);
                return resolver;
            }
        } : new CoreSchema();
        return LoadSettings.builder().setSchema(schema)
                .setAllowDuplicateKeys(Boolean.parseBoolean(System.getProperty(ALLOW_DUPLICATE_KEYS, "true")))
                .build();
    }

    /** Creates a reader that leaves encrypted strings unchanged. Readers are not thread safe. */
    public static Load newLoad() {
        return new Load(loadSettings(false));
    }

    /** Creates a reader using the configured decryptor for unquoted CRYPT scalars. */
    public static Load newLoad(String decryptorClass) {
        if (decryptorClass == null || decryptorClass.isBlank()) {
            return newLoad();
        }
        return new Load(loadSettings(true), DecryptConstructor.getInstance(decryptorClass));
    }

    /** Creates a block-style writer with the same scalar schema as the readers. */
    public static Dump newDump() {
        DumpSettings settings = DumpSettings.builder().setSchema(new CoreSchema())
                .setDefaultFlowStyle(FlowStyle.BLOCK).build();
        StandardRepresenter representer = new StandardRepresenter(settings) {
            {
                parentClassRepresenters.put(Date.class, value ->
                        representScalar(Tag.STR, Instant.ofEpochMilli(((Date) value).getTime()).toString()));
                parentClassRepresenters.put(Calendar.class, value ->
                        representScalar(Tag.STR, ((Calendar) value).toInstant().toString()));
                parentClassRepresenters.put(TemporalAccessor.class, value -> representScalar(Tag.STR, value.toString()));
                parentClassRepresenters.put(Enum.class, value -> representScalar(Tag.STR, ((Enum<?>) value).name()));
            }
        };
        return new Dump(settings, representer);
    }

    /** Parse with exclusive access to a shared Engine reader. */
    public static Object load(Load reader, InputStream input) {
        synchronized (reader) {
            return reader.loadFromInputStream(input);
        }
    }

    /** Parse with exclusive access to a shared Engine reader. */
    public static Object load(Load reader, String input) {
        synchronized (reader) {
            return reader.loadFromString(input);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> loadMap(Load reader, InputStream input) {
        return (Map<String, Object>) load(reader, input);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> loadMap(Load reader, String input) {
        return (Map<String, Object>) load(reader, input);
    }
}
